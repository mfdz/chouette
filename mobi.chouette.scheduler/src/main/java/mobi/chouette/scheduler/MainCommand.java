package mobi.chouette.scheduler;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.text.MessageFormat;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.ws.rs.core.MediaType;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.JSONUtils;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.JobDAO;
import mobi.chouette.model.api.Job;
import mobi.chouette.model.api.Job.STATUS;
import mobi.chouette.model.api.Link;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

@Log4j
@Stateless(name = MainCommand.COMMAND)
public class MainCommand implements Command, Constant {

	public static final String COMMAND = "MainCommand";

	@EJB
	JobDAO jobDAO;
	

	@Override
	public boolean execute(Context context) throws Exception {
		boolean result = false;

		URI baseURI = (URI) context.get(BASE_URI);

		Long id = (Long) context.get(JOB_ID);
		Job job = jobDAO.find(id);

		context.put(PATH, job.getPath());
		context.put(ARCHIVE, job.getFilename());
		java.nio.file.Path path = Paths.get(System.getProperty("user.home"),
				ROOT_PATH, job.getReferential(), "data",
				job.getId().toString(), PARAMETERS_FILE);
		Parameters parameters = JSONUtils.fromJSON(path, Parameters.class);
		context.put(PARAMETERS, parameters);
		context.put(CONFIGURATION, parameters.getConfiguration());
		context.put(VALIDATION, parameters.getValidation());
		context.put(JOB_REFERENTIAL, job.getReferential());
		context.put(ACTION, job.getAction());
		context.put(TYPE, job.getType());

		String type = job.getType() == null ? "" : job.getType();

		String name = "mobi.chouette.exchange."
				+ (type.isEmpty() ? "" : type + ".") + job.getAction() + "."
				+ StringUtils.capitalize(type)
				+ StringUtils.capitalize(job.getAction()) + "Command";

		InitialContext ctx = (InitialContext) context.get(INITIAL_CONTEXT);
		Command command = CommandFactory.create(ctx, name);
		command.execute(context);

		job.setStatus(STATUS.TERMINATED);

		// remove location cancellink
		Iterables.removeIf(job.getLinks(), new Predicate<Link>() {
			@Override
			public boolean apply(Link link) {
				return link.getRel().equals(Link.LOCATION_REL) 
						|| link.getRel().equals(Link.CANCEL_REL)  ;
			}
		});

		// add location link
		Link link = new Link();
		link.setType(MediaType.APPLICATION_JSON);
		link.setRel(Link.LOCATION_REL);
		link.setMethod(Link.GET_METHOD);
		String href = MessageFormat.format("{0}/{1}/reports/{2,number,#}",
				ROOT_PATH, job.getReferential(), job.getId());
		link.setHref(baseURI.toASCIIString() + href);
		job.getLinks().add(link);

		// add delete link
		link = new Link();
		link.setType("application/json");
		link.setRel(Link.DELETE_REL);
		link.setMethod(Link.DELETE_METHOD);
		href = MessageFormat.format("{0}/{1}/reports/{2,number,#}",
				ROOT_PATH, job.getReferential(), job.getId());
		link.setHref(baseURI.toASCIIString() + href);
		job.getLinks().add(link);

		// add validation link
		link = new Link();
		link.setType(MediaType.APPLICATION_JSON);
		link.setRel(Link.VALIDATION_REL);
		link.setMethod(Link.GET_METHOD);
		href = MessageFormat.format(
				"{0}/{1}/data/{2,number,#}/validation.json", ROOT_PATH,
				job.getReferential(), job.getId());
		link.setHref(baseURI.toASCIIString() + href);
		job.getLinks().add(link);

		// add data upload link
		if (job.getAction().equals(EXPORTER)) {

			href = MessageFormat.format(
					"{0}/{1}/data/{2,number,#}/{3}", ROOT_PATH,
					job.getReferential(), job.getId(), job.getFilename());

			// if (Files.exists(Paths.get(System.getProperty("user.home"), href))) {
				link = new Link();
				link.setType(MediaType.APPLICATION_OCTET_STREAM);
				link.setRel(Link.DATA_REL);
				link.setMethod(Link.GET_METHOD);
				link.setHref(baseURI.toASCIIString() + href);

				job.getLinks().add(link);
			//}
		}

		jobDAO.update(job);

		return result;
	}

	public static class DefaultCommandFactory extends CommandFactory {

		@Override
		protected Command create(InitialContext context) throws IOException {
			Command result = null;
			try {
				String name = "java:app/mobi.chouette.scheduler/" + COMMAND;
				result = (Command) context.lookup(name);
			} catch (Exception e) {
				log.error(e);
			}
			return result;
		}
	}

	static {
		CommandFactory.factories.put(MainCommand.class.getName(),
				new DefaultCommandFactory());
	}
}
