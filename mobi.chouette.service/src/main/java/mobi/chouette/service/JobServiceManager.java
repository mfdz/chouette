package mobi.chouette.service;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ws.rs.core.MediaType;

import lombok.extern.log4j.Log4j;
import mobi.chouette.dao.JobDAO;
import mobi.chouette.dao.SchemaDAO;
import mobi.chouette.model.api.Job;
import mobi.chouette.model.api.Job.STATUS;
import mobi.chouette.model.api.Link;
import mobi.chouette.scheduler.Scheduler;

import org.apache.commons.io.FileUtils;

@Singleton(name = JobServiceManager.BEAN_NAME)
@Startup
@Log4j
public class JobServiceManager {

	public static final String BEAN_NAME = "JobServiceManager";

	@EJB
	JobDAO jobDAO;

	@EJB
	SchemaDAO schemaDAO;

	@EJB
	Scheduler scheduler;

	public JobService create(String referential, String action, String type, Map<String, InputStream> inputStreamsByName) throws ServiceException {
		JobService jobService = null;
		try {
			// Valider les parametres
			validateReferential(referential);

			// Instancier le modèle du service 'upload'
			jobService = new JobService(referential, action, type);

			// Enregistrer le jobService pour obtenir un id
			jobDAO.create(jobService.getJob());
			jobDAO.flush();

			// mkdir
			if (Files.exists( jobService.getPath())) {
				// réutilisation anormale d'un id de job (réinitialisation de la séquence à l'extérieur de l'appli?) 
				// jobDAO.delete( jobService.getJob());
				FileUtils.deleteDirectory(jobService.getPath().toFile());
			}
			Files.createDirectories( jobService.getPath());

			// Enregistrer des paramètres à conserver sur fichier
			jobService.saveInputStreams( inputStreamsByName);

			jobDAO.update( jobService.getJob());
			jobDAO.flush();

			// Lancer la tache
			scheduler.schedule(jobService.getReferential());

			return jobService;

		} catch (RequestServiceException ex) {
			deleteBadCreatedJob(jobService);
			throw ex;
		} catch (Exception ex) {
			Logger.getLogger(JobServiceManager.class.getName()).log(Level.INFO, "", ex);

			deleteBadCreatedJob(jobService);

			throw new ServiceException(ServiceExceptionCode.INTERNAL_ERROR, ex);
		}
	}

	private void deleteBadCreatedJob(JobService jobService)
	{
		if (jobService==null || jobService.getJob().getId() == null) return; 
		jobDAO.delete(jobService.getJob());

		try {
			// remove path if exists
			if ( jobService.getPath() != null && Files.exists( jobService.getPath()))
				FileUtils.deleteDirectory( jobService.getPath().toFile());
		} catch (IOException ex1) {
			Logger.getLogger(JobServiceManager.class.getName()).log(Level.SEVERE, null, ex1);
		}

	}

	private void validateReferential(String referential) throws ServiceException {
		if (!schemaDAO.getSchemaListing().contains(referential)) {
			throw new RequestServiceException(RequestExceptionCode.UNKNOWN_REFERENTIAL, "");
		}
	}

	public JobService download( String referential, Long id, String filename) throws ServiceException {
		JobService jobService = getJobService( referential, id);

		java.nio.file.Path path = Paths.get( jobService.getPathName(), filename);
		if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
			throw new RequestServiceException(RequestExceptionCode.UNKNOWN_FILE, "");
		}
		return jobService;
	}

	/**
	 * find next waiting job on referential <br/>
	 * return null if a job is STARTED or if no job is SCHEDULED
	 *
	 * @param referential
	 * @return
	 */
	// @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public JobService getNextJob(String referential) {
		Job job = jobDAO.getNextJob(referential);
		if (job == null) {
			return null;
		}
		return new JobService(job);
	}

	public void start(JobService jobService) {
		jobService.setStatus(STATUS.STARTED);
		jobService.setUpdated(new Date());
		jobService.setStarted(new Date());
		jobService.addLink(MediaType.APPLICATION_JSON, Link.REPORT_REL);
		jobService.addLink(MediaType.APPLICATION_JSON, Link.VALIDATION_REL);
		jobDAO.update(jobService.getJob());
	}

	// @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public void cancel(String referential, Long id) throws ServiceException {
		JobService jobService = getJobService(referential, id);
		if (jobService.getStatus().ordinal() <= STATUS.STARTED.ordinal()) {

			if (jobService.getStatus().equals(STATUS.STARTED)) {
				scheduler.cancel(jobService);
			}

			jobService.setStatus(STATUS.CANCELED);

			// remove cancel link only
			jobService.removeLink(Link.CANCEL_REL);
			// set delete link
			jobService.addLink(MediaType.APPLICATION_JSON, Link.DELETE_REL);

			jobService.setUpdated(new Date());
			jobDAO.update(jobService.getJob());

		}

	}

	// @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public void remove(String referential, Long id) throws ServiceException {
		JobService jobService = getJobService(referential, id);
		if (jobService.getStatus().ordinal() <= STATUS.STARTED.ordinal()) {
			throw new RequestServiceException(RequestExceptionCode.SCHEDULED_JOB, "referential = " + referential + " ,id = " + id);
		}
		try {
			FileUtils.deleteDirectory(jobService.getPath().toFile());
		} catch (IOException e) {
			Logger.getLogger(JobServiceManager.class.getName()).log(Level.SEVERE, "fail to delete directory", e);
		}

		jobDAO.delete(jobService.getJob());
	}

	// @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public void drop(String referential) throws ServiceException {
		List<JobService> jobServices = findAll(referential);
		// supprimer en premier les jobs en attente, puis les autres
		for (Iterator<JobService> iterator = jobServices.iterator(); iterator.hasNext();) {
			JobService jobService = iterator.next();
			if (jobService.getStatus().equals(STATUS.SCHEDULED)) {
				jobDAO.delete(jobService.getJob());
				try {
					FileUtils.deleteDirectory(jobService.getPath().toFile());
				} catch (IOException e) {
					Logger.getLogger(JobServiceManager.class.getName()).log(Level.SEVERE, "fail to delete directory", e);
				}
				iterator.remove();
			}
		}
		for (JobService jobService : jobServices) {
			if (jobService.getStatus().equals(STATUS.STARTED)) {
				scheduler.cancel(jobService);
			}
			try {
				FileUtils.deleteDirectory(jobService.getPath().toFile());
			} catch (IOException e) {
				Logger.getLogger(JobServiceManager.class.getName()).log(Level.SEVERE, "fail to delete directory", e);
			}
			jobDAO.delete(jobService.getJob());
		}

	}

	public void terminate(JobService jobService) {
		jobService.setStatus(STATUS.TERMINATED);

		// remove cancel link only
		jobService.removeLink(Link.CANCEL_REL);
		// set delete link
		jobService.addLink(MediaType.APPLICATION_JSON, Link.DELETE_REL);
		// add data link if necessary
		if (!jobService.linkExists(Link.DATA_REL)) {
			if (jobService.getFilename() != null && Files.exists(Paths.get(jobService.getPathName(), jobService.getFilename()))) {
				jobService.addLink(MediaType.APPLICATION_OCTET_STREAM, Link.DATA_REL);
			}
		}

		jobService.setUpdated(new Date());
		jobDAO.update(jobService.getJob());

	}

	public void abort(JobService jobService) {

		jobService.setStatus(STATUS.ABORTED);

		// remove cancel link only
		jobService.removeLink(Link.CANCEL_REL);
		// set delete link
		jobService.addLink(MediaType.APPLICATION_JSON, Link.DELETE_REL);

		jobService.setUpdated(new Date());
		jobDAO.update(jobService.getJob());

	}

	public List<JobService> findAll() {
		List<Job> jobs = jobDAO.findAll();
		List<JobService> jobServices = new ArrayList<>(jobs.size());
		for (Job job : jobs) {
			jobServices.add(new JobService(job));
		}
		return jobServices;
	}

	public List<JobService> findAll(String referential) {
		List<Job> jobs = jobDAO.findByReferential(referential);
		List<JobService> jobServices = new ArrayList<>(jobs.size());
		for (Job job : jobs) {
			jobServices.add(new JobService(job));
		}

		return jobServices;
	}

	public JobService scheduledJob(String referential, Long id) throws ServiceException {
		return getJobService( referential, id);
	}


	public JobService terminatedJob(String referential, Long id) throws ServiceException {
		JobService jobService = getJobService( referential, id);

		if (jobService.getStatus().ordinal() < STATUS.TERMINATED.ordinal()
				|| jobService.getStatus().ordinal() == STATUS.DELETED.ordinal()) {
			throw new RequestServiceException(RequestExceptionCode.UNKNOWN_JOB, "referential = " + referential + " ,id = " + id);
		}

		return jobService;
	}

	private JobService getJobService(String referential, Long id) throws ServiceException {
		validateReferential( referential);

		Job job = jobDAO.find(id);
		if (job != null && job.getReferential().equals(referential)) {
			return new JobService(job);
		}
		throw new RequestServiceException(RequestExceptionCode.UNKNOWN_JOB, "referential = " + referential + " ,id = " + id);
	}

	public JobService getJobService( Long id) throws ServiceException {
		Job job = jobDAO.find(id);
		if (job != null) {
			return new JobService(job);
		}
		throw new RequestServiceException(RequestExceptionCode.UNKNOWN_JOB, " id = " + id);
	}

	public List<JobService> jobs(String referential, String action, final Long version) throws ServiceException {
		validateReferential( referential);

		List<Job> jobs = null;
		if ( action==null) {
			jobs = jobDAO.findByReferential(referential);
		} else {
			jobs = jobDAO.findByReferentialAndAction(referential, action);
		}

		Collection<Job> filtered = Collections2.filter( jobs, new Predicate<Job>() {
			@Override
			public boolean apply(Job job) {
				// filter on update time if given, otherwise don't return
						// deleted jobs
				boolean versionZeroCondition = ( version == 0) && job.getStatus().ordinal() < STATUS.DELETED.ordinal();
				boolean versionNonZeroCondition = (version > 0) && version < job.getUpdated().getTime();

				return versionZeroCondition || versionNonZeroCondition;
			}
		});

		List<JobService> jobServices = new ArrayList<>(filtered.size());
		for (Job job : filtered) {
			jobServices.add(new JobService(job));
		}
		return jobServices;
	}


}
