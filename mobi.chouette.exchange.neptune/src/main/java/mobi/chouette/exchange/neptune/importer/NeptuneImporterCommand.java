package mobi.chouette.exchange.neptune.importer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.FileUtils;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.importer.CopyCommand;
import mobi.chouette.exchange.importer.RegisterCommand;
import mobi.chouette.exchange.importer.UncompressCommand;
import mobi.chouette.exchange.report.FileInfo;
import mobi.chouette.exchange.report.Report;
import mobi.chouette.exchange.validation.report.ValidationReport;

import com.google.common.collect.Lists;
import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;

@Stateless(name = NeptuneImporterCommand.COMMAND)
@ToString
@Log4j
public class NeptuneImporterCommand implements Command, Constant {

	public static final String COMMAND = "NeptuneImporterCommand";

	@Resource(lookup = "java:comp/DefaultManagedExecutorService")
	ManagedExecutorService executor;
	
	
	@Override
	public boolean execute(Context context) throws Exception {
		boolean result = ERROR;
		Monitor monitor = MonitorFactory.start(COMMAND);

		try {
			InitialContext initialContext = (InitialContext) context
					.get(INITIAL_CONTEXT);
					
			// report
			Report report = new Report();			
			ValidationReport validationReport = new ValidationReport();
			context.put(Constant.REPORT, report);
			context.put(Constant.VALIDATION_REPORT, validationReport);
			
			// uncompress data
			Command uncompress = CommandFactory.create(initialContext,
					UncompressCommand.class.getName());
			uncompress.execute(context);

			Path path = Paths.get(context.get(PATH).toString(), INPUT);
			List<Path> stream = FileUtils.listFiles(path, "*.xml");

			// validation
			Monitor validation = MonitorFactory.start("Parallel" + NeptuneSAXParserCommand.COMMAND );			
			int n = Runtime.getRuntime().availableProcessors() / 2;
			int size = (int )Math.ceil(stream.size() / n);
			List<List<Path>> partition = Lists.partition(stream, size);
			List<Task> tasks = new ArrayList<Task>();
			for (int i = 0; i < n; i++) {
				tasks.add(new Task(initialContext, context, partition.get(i)));
			}
			List<Future<Boolean>> futures = executor.invokeAll(tasks);
			log.info(Color.YELLOW + validation.stop() + Color.NORMAL);

			for (Path file : stream) {

				// skip metadata file
				if (file.toFile().getName().toLowerCase().contains("metadata"))
				{
					FileInfo fileItem = new FileInfo();
					fileItem.setName(file.toFile().getName());
					fileItem.setStatus(FileInfo.STATE.UNCHECKED);
					report.getFiles().getFileInfos().add(fileItem);
					continue;
				}
				
				log.info("[DSU] import : " + file.toString());				
				context.put(FILE_URL, file.toUri().toURL().toExternalForm());
				
				// parser
				Command parser = CommandFactory.create(initialContext,
						NeptuneParserCommand.class.getName());
				parser.execute(context);

				// validation
				Command validator = CommandFactory.create(initialContext,
						NeptuneValidationCommand.class.getName());
				validator.execute(context);

				// register
				Command register = CommandFactory.create(initialContext,
						RegisterCommand.class.getName());
				register.execute(context);
				
				Command copy = CommandFactory.create(initialContext,
						CopyCommand.class.getName());
				copy.execute(context);

			}

			// save report 
			
			result = SUCCESS;
		} catch (Exception e) {
			log.error(e);
		}

		log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
		return result;
	}

	@AllArgsConstructor
	class Task implements Callable<Boolean> {

		private InitialContext initialContext;
		private Context context;
		private List<Path> files = new ArrayList<Path>();

		@Override
		public Boolean call() throws Exception {
			boolean result = SUCCESS;
			log.info("[DSU] validate : " + files.size());

			for (Path file : files) {
				context.put(FILE_URL, file.toUri().toURL().toExternalForm());
				Command validation = CommandFactory.create(this.initialContext,
						NeptuneSAXParserCommand.class.getName());
				result = validation.execute(context);
			}
			return result;
		}
	}

	public static class DefaultCommandFactory extends CommandFactory {

		@Override
		protected Command create(InitialContext context) throws IOException {
			Command result = null;
			try {
				String name = "java:app/mobi.chouette.exchange.neptune/"
						+ COMMAND;
				result = (Command) context.lookup(name);
			} catch (NamingException e) {
				log.error(e);
			}
			return result;
		}
	}

	static {
		CommandFactory factory = new DefaultCommandFactory();
		CommandFactory.factories.put(NeptuneImporterCommand.class.getName(),
				factory);
	}
}