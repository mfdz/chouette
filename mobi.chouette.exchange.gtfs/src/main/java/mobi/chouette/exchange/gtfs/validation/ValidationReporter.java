package mobi.chouette.exchange.gtfs.validation;

import java.util.List;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.gtfs.model.importer.GtfsException;
import mobi.chouette.exchange.report.ActionReport;
import mobi.chouette.exchange.report.FileError;
import mobi.chouette.exchange.report.FileInfo.FILE_STATE;
import mobi.chouette.exchange.validation.report.CheckPoint;
import mobi.chouette.exchange.validation.report.Location;
import mobi.chouette.exchange.validation.report.ValidationReport;

public class ValidationReporter implements Constant {

	public void reportErrors(Context context, List<GtfsException> errors, String filename) throws Exception {
		for (GtfsException error : errors) {
			reportError(context, error, filename);
		}
	}
	
	public void reportError(Context context, GtfsException ex, String filenameInfo) throws Exception {
		ActionReport report = (ActionReport) context.get(REPORT);
		ValidationReport validationReport = (ValidationReport) context.get(MAIN_VALIDATION_REPORT);
		String name = name(filenameInfo);
		String checkPointName = "";
		String fieldName = "";

		switch ( ex.getError() ) {
		case INVALID_HEADER_FILE_FORMAT:
			report.addFileInfo(filenameInfo, FILE_STATE.ERROR,
					new FileError(FileError.CODE.INVALID_FORMAT,
							"The first line in file \""+filenameInfo+"\" must comply with CSV (rule 1-GTFS-CSV-10"));
			validationReport.addDetail(GTFS_1_GTFS_CSV_10,
					new Location(filenameInfo, name(filenameInfo)+"-failure", ((GtfsException) ex).getId()),
					"The first line in file \""+filenameInfo+"\" must comply with CSV",
					CheckPoint.RESULT.NOK);
			throw new Exception("The first line in file \""+filenameInfo+"\" must comply with CSV");
			
		case EXTRA_SPACE_IN_HEADER_FIELD: // Don't throw an exception at this level
			report.addFileInfo(filenameInfo, FILE_STATE.IGNORED,
					new FileError(FileError.CODE.INVALID_FORMAT,
							"Extra spaces in field names are not allowed (rule 1-GTFS-CSV-7"));
			validationReport.addDetail(GTFS_1_GTFS_CSV_7,
					new Location(filenameInfo, "Extra spaces in field names are not allowed", ((GtfsException) ex).getId()),
					"Extra spaces in field names are not allowed",
					CheckPoint.RESULT.NOK);
			break;
			
		case EXTRA_HEADER_FIELD: // 1_GTFS_Agency_10, 1_GTFS_Stop_11, 1-GTFS-Route-10, 1-GTFS-StopTime-12, 1-GTFS-Trip-8, 1-GTFS-Frequency-7, 1-GTFS-Calendar-14, 1-GTFS-CalendarDate-7, 1-GTFS-Transfer-6 info
			checkPointName = checkPointName(name, GtfsException.ERROR.EXTRA_HEADER_FIELD);
			report.addFileInfo(filenameInfo, FILE_STATE.IGNORED,
					new FileError(FileError.CODE.INVALID_FORMAT,
							"Extra fields are provided (rule "+checkPointName+")"));
			validationReport.addDetail(checkPointName,
					new Location(filenameInfo, "Extra fields are provided", ((GtfsException) ex).getId(), ((GtfsException) ex).getField()),
					"Extra fields are provided",
					CheckPoint.RESULT.NOK);
			break;
			
		case HTML_TAG_IN_HEADER_FIELD:
			report.addFileInfo(filenameInfo, FILE_STATE.ERROR,
					new FileError(FileError.CODE.INVALID_FORMAT,
							"HTML tags in field names are not allowed (rule 1-GTFS-CSV-6"));
			validationReport.addDetail(GTFS_1_GTFS_CSV_6,
					new Location(filenameInfo, "HTML tags in field names are not allowed", ((GtfsException) ex).getId()),
					"HTML tags in field names are not allowed",
					CheckPoint.RESULT.NOK);
			break;
			
		case EMPTY_HEADER_FIELD:
			report.addFileInfo(filenameInfo, FILE_STATE.ERROR,
					new FileError(FileError.CODE.INVALID_FORMAT,
							"Header fields in file \""+filenameInfo+"\" could not be empty (rule 1-GTFS-CSV-11"));
			validationReport.addDetail(GTFS_1_GTFS_CSV_11,
					new Location(filenameInfo, name(filenameInfo)+"-failure", ((GtfsException) ex).getId()),
					"Header fields in file \""+filenameInfo+"\" could not be empty",
					CheckPoint.RESULT.NOK);
			throw new Exception("Header fields in file \""+filenameInfo+"\" could not be empty");

		case DUPLICATE_HEADER_FIELD:
			report.addFileInfo(filenameInfo, FILE_STATE.ERROR,
					new FileError(FileError.CODE.INVALID_FORMAT,
							"The header fields in file \""+filenameInfo+"\" could not be duplicated (rule 1-GTFS-CSV-12"));
			validationReport.addDetail(GTFS_1_GTFS_CSV_12,
					new Location(filenameInfo, name(filenameInfo)+"-failure", ((GtfsException) ex).getId()),
					"The header fields in file \""+filenameInfo+"\" could not be duplicated",
					CheckPoint.RESULT.NOK);
			throw new Exception("The header fields in file \""+filenameInfo+"\" could not be duplicated");
		
		case MISSING_REQUIRED_FIELDS: // 1_GTFS_Agency_2, 1_GTFS_Agency_4, 1-GTFS-Stop-2, 1-GTFS-Route-2, 1-GTFS-StopTime-2, 1-GTFS-Trip-2, 1-GTFS-Frequency-1, 1-GTFS-Calendar-2, 1-GTFS-CalendarDate-2, 1-GTFS-Transfer-1 error
			checkPointName = checkPointName(name, GtfsException.ERROR.MISSING_REQUIRED_FIELDS);
			fieldName = ex.getField();
			report.addFileInfo(filenameInfo, FILE_STATE.ERROR,
					new FileError(FileError.CODE.INVALID_FORMAT,
							"The field \""+fieldName+"\" must be provided (rule "+checkPointName+")"));
			validationReport.addDetail(checkPointName,
					new Location(filenameInfo, name+"-failure", ((GtfsException) ex).getId()),
					"The fields \""+fieldName+"\" must be provided",
					CheckPoint.RESULT.NOK);
			if (fieldName != null && fieldName.endsWith("_id"))
				throw new Exception("The fields \""+fieldName+"\" must be provided");
			break;				

		case MISSING_FIELD: // 1-GTFS-Agency-2, 1-GTFS-Stop-2, 
			checkPointName = checkPointName(name, GtfsException.ERROR.MISSING_FIELD);
			fieldName = ex.getField();
			report.addFileInfo(filenameInfo, FILE_STATE.ERROR,
					new FileError(FileError.CODE.INVALID_FORMAT,
							"The file \""+filenameInfo+"\" must provide a non empty \""+name+"_id\" for each "+name+" (rule "+checkPointName+")"));
			validationReport.addDetail(checkPointName,
					new Location(filenameInfo, name+"-failure", ((GtfsException) ex).getId()),
					"The file \""+filenameInfo+"\" must provide a non empty \""+fieldName+"\" for each "+name,
					CheckPoint.RESULT.NOK);
			throw new Exception("The file \""+filenameInfo+"\" must provide a non empty \""+fieldName+"\" for each "+name);

/////////////////////////			
		case MISSING_REQUIRED_VALUES: // 1-GTFS-Agency-5 
			checkPointName = checkPointName(name, GtfsException.ERROR.MISSING_FIELD);
			fieldName = ex.getField();
			report.addFileInfo(filenameInfo, FILE_STATE.ERROR,
					new FileError(FileError.CODE.INVALID_FORMAT,
							"The value \""+fieldName+"\" must be provided (rule "+checkPointName+")"));
			validationReport.addDetail(checkPointName,
					new Location(filenameInfo, name(filenameInfo)+"-failure", ((GtfsException) ex).getId()),
					"The value \""+fieldName+"\" must be provided",
					CheckPoint.RESULT.NOK);
			//throw new Exception("The value \""+fieldName+"\" must be provided");
			break;

			
////////////////////////////			
		case FILE_WITH_NO_ENTRY:
			report.addFileInfo(filenameInfo, FILE_STATE.ERROR,
					new FileError(FileError.CODE.INVALID_FORMAT,
							"The file \""+filenameInfo+"\" must contain at least one agency definition (rule 1-GTFS-Agency-11)"));
			validationReport.addDetail(GTFS_1_GTFS_Agency_11,
					new Location(filenameInfo, name(filenameInfo)+"-failure", ((GtfsException) ex).getId()),
					"The file \""+filenameInfo+"\" must contain at least one agency definition",
					CheckPoint.RESULT.NOK);
			throw new Exception("The file \""+filenameInfo+"\" must contain at least one agency definition");
		case DUPLICATE_DEFAULT_KEY_FIELD:
			// TODO. Give the rigth code : At most only one Agency can have default value agency_id
			report.addFileInfo(filenameInfo, FILE_STATE.ERROR,
					new FileError(FileError.CODE.INVALID_FORMAT, "At most only one Agency can have default value \"agency_id\" (rule 1-GTFS-Agency-5)"));
			validationReport.addDetail(GTFS_1_GTFS_Agency_5,
					new Location(filenameInfo, name(filenameInfo)+"-failure", ((GtfsException) ex).getId(), ((GtfsException) ex).getField()),
					"At most only one Agency can have default value \"agency_id\"",
					CheckPoint.RESULT.NOK);
			throw new Exception("At most only one Agency can have default value \"agency_id\"");
		case DUPLICATE_FIELD:
			report.addFileInfo(filenameInfo, FILE_STATE.ERROR,
					new FileError(FileError.CODE.INVALID_FORMAT, "The field \"agency_id\" must be unique (rule 1-GTFS-Agency-3)"));
			validationReport.addDetail(GTFS_1_GTFS_Agency_3,
					new Location(filenameInfo, name(filenameInfo)+"-failure", ((GtfsException) ex).getId(), ((GtfsException) ex).getField()),
					"The field \"agency_id\" must be unique",
					CheckPoint.RESULT.NOK);
			throw new Exception("The field \"agency_id\" must be unique");
		case INVALID_FILE_FORMAT:
			report.addFileInfo(filenameInfo, FILE_STATE.ERROR,
					new FileError(FileError.CODE.INVALID_FORMAT,
							"Line number "+((GtfsException) ex).getId()+" in file \""+filenameInfo+"\" must comply with CSV (rule 1-GTFS-CSV-13"));
			validationReport.addDetail(GTFS_1_GTFS_CSV_13,
					new Location(filenameInfo, name(filenameInfo)+"-failure", ((GtfsException) ex).getId()),
					"Line number "+((GtfsException) ex).getId()+" in file \""+filenameInfo+"\" must comply with CSV",
					CheckPoint.RESULT.NOK);
			throw new Exception("Line number "+((GtfsException) ex).getId()+" in file \""+filenameInfo+"\" must comply with CSV");
		case EXTRA_SPACE_IN_FIELD: // Don't throw an exception at this level
			report.addFileInfo(filenameInfo, FILE_STATE.IGNORED,
					new FileError(FileError.CODE.INVALID_FORMAT,
							"Extra spaces in field names are not allowed (rule 1-GTFS-CSV-7"));
			validationReport.addDetail(GTFS_1_GTFS_CSV_7,
					new Location(filenameInfo, "Extra spaces in field names are not allowed", ((GtfsException) ex).getId()),
					"Extra spaces in field names are not allowed",
					CheckPoint.RESULT.NOK);
			break;
			
		case INVALID_URL:// 1-GTFS-Agency-7  warning
			report.addFileInfo(filenameInfo, FILE_STATE.IGNORED,
					new FileError(FileError.CODE.INVALID_FORMAT,
							"Invalid URL (rule 1-GTFS-Agency-7"));
			validationReport.addDetail(GTFS_1_GTFS_Agency_7,
					new Location(filenameInfo, "Invalid URL", ((GtfsException) ex).getId(), ((GtfsException) ex).getField()),
					"Invalid URL",
					CheckPoint.RESULT.NOK);
			break;
		case INVALID_TIMEZONE:// 1-GTFS-Agency-6  warning
			report.addFileInfo(filenameInfo, FILE_STATE.IGNORED,
					new FileError(FileError.CODE.INVALID_FORMAT,
							"Invalid time zone (rule 1-GTFS-Agency-6"));
			validationReport.addDetail(GTFS_1_GTFS_Agency_6,
					new Location(filenameInfo, "Invalid time zone", ((GtfsException) ex).getId(), ((GtfsException) ex).getField()),
					"Invalid time zone",
					CheckPoint.RESULT.NOK);
			break;
		case INVALID_FARE_URL:// 1-GTFS-Agency-9   warning
			report.addFileInfo(filenameInfo, FILE_STATE.IGNORED,
					new FileError(FileError.CODE.INVALID_FORMAT,
							"Invalid fare URL (rule 1-GTFS-Agency-9"));
			validationReport.addDetail(GTFS_1_GTFS_Agency_9,
					new Location(filenameInfo, "Invalid fare URL", ((GtfsException) ex).getId(), ((GtfsException) ex).getField()),
					"Invalid fare URL",
					CheckPoint.RESULT.NOK);
			break;
		case INVALID_LANG: // 1-GTFS-Agency-8   warning
			report.addFileInfo(filenameInfo, FILE_STATE.IGNORED,
					new FileError(FileError.CODE.INVALID_FORMAT,
							"Invalid lang (rule 1-GTFS-Agency-8"));
			validationReport.addDetail(GTFS_1_GTFS_Agency_8,
					new Location(filenameInfo, "Invalid lang", ((GtfsException) ex).getId(), ((GtfsException) ex).getField()),
					"Invalid lang",
					CheckPoint.RESULT.NOK);
			break;
			
		case INVALID_FORMAT:
			break;
		case MISSING_FILE:
			break;
		case MISSING_FOREIGN_KEY:
			break;
		case SYSTEM:
			break;
		default:
			break;

//		case MISSING_FILE: // THIS CAN NEVER OCCUR ! Already checked in importer.hasAgencyImporter()
//			report.addFileInfo(filename, FILE_STATE.ERROR,
//					new FileError(FileError.CODE.FILE_NOT_FOUND, "The file \""+filename+"\" must be provided (rule 1-GTFS-Agency-1)"));
//			validationReport.addDetail(GTFS_1_GTFS_Agency_1,
//					new Location(filename, name(filename)+"-failure"),
//					"The file \""+filename+"\" must be provided",
//					CheckPoint.RESULT.NOK);
//			throw new Exception("The file \""+filename+"\" must be provided");
//		
//		case INVALID_FORMAT: // THIS CAN NEVER OCCUR !
//		case MISSING_FOREIGN_KEY: // THIS CAN NEVER OCCUR !
//		case SYSTEM: // Problem while openning file \""+filename+"\"
//		case MISSING_REQUIRED_VALUES: // This cannot occur at this place
//			;
			
//		default:
//			throwUnknownError(report, validationReport);

		}
	}
	
	private String checkPointName(String name, mobi.chouette.exchange.gtfs.model.importer.GtfsException.ERROR errorName) {
		// 1_GTFS_Agency_10, 1_GTFS_Stop_11, 1-GTFS-Route-10, 1-GTFS-StopTime-12, 1-GTFS-Trip-8, 1-GTFS-Frequency-7, 1-GTFS-Calendar-14, 1-GTFS-CalendarDate-7, 1-GTFS-Transfer-6 info
		//checkPointName = checkPointName(name, GtfsException.ERROR.EXTRA_HEADER_FIELD);
		name = capitalize(name);
		switch(errorName) {
		case EXTRA_HEADER_FIELD:
			if ("Agency".equals(name))
				return "1-GTFS-"+name+"-10";
			else
				return "1-GTFS-"+name+"-11";
		case MISSING_REQUIRED_FIELDS:
			if ("Agency".equals(name))
				return "1-GTFS-"+name+"-4";
			return "1-GTFS-"+name+"-2";
		case MISSING_FIELD: // 1-GTFS-Agency-2, 1-GTFS-Stop-2,
			if ("Agency".equals(name))
				return "1-GTFS-"+name+"-2";
			return "1-GTFS-"+name+"-3";
		case MISSING_REQUIRED_VALUES:
			if ("Agency".equals(name))
				return "1-GTFS-"+name+"-5";
			return "1-GTFS-"+name+"-3";
		default:
			return null;
		}
	}

	private String capitalize(String name) {
		// CSV, CalendarDate, StopTime
		if ("csv".equalsIgnoreCase(name))
			return "CSV";
		if ("calendardate".equalsIgnoreCase(name))
			return "CalendarDate";
		if ("stoptime".equalsIgnoreCase(name))
			return "StopTime";
		if (name != null && !name.trim().isEmpty()) {
			name = name.trim();
			char c = name.charAt(0);
			if (c >= 'a' && c <= 'z') {
				name = name.substring(1);
				name = (char)((int)c+(int)'A'-(int)('a')) + name;
			}
		}
		return name;
	}

	private String name(String filename) {
		if (filename != null) {
			if (filename.indexOf('.') > 0)
				filename = filename.substring(0, filename.lastIndexOf('.'));
			if (filename.endsWith("s"))
				filename = filename.substring(0, filename.lastIndexOf('s'));
			return filename;
		}
		return "";
	}
	
	public void throwUnknownError(Context context, Exception ex, String filenameInfo) throws Exception {
		ActionReport report = (ActionReport) context.get(REPORT);
		ValidationReport validationReport = (ValidationReport) context.get(MAIN_VALIDATION_REPORT);

		if (filenameInfo != null && filenameInfo.indexOf('.') > 0) {
			report.addFileInfo(filenameInfo, FILE_STATE.ERROR,
					new FileError(FileError.CODE.FILE_NOT_FOUND, "A problem occured while reading the file \""+filenameInfo+"\" (rule 1-GTFS-CSV-14) : "+ex.getMessage()));
			validationReport.addDetail(GTFS_1_GTFS_CSV_14,
					new Location(filenameInfo, filenameInfo.substring(0, filenameInfo.lastIndexOf('.'))+"-failure"),
					"A problem occured while reading the file \""+filenameInfo+"\" : "+ex.getMessage(),
					CheckPoint.RESULT.NOK);
			throw new Exception("A problem occured while reading the file \""+filenameInfo+"\" : "+ex.getMessage());
		}
	}
	
	public void reportSuccess(Context context, String checkpointName, String filenameInfo) {
		ActionReport report = (ActionReport) context.get(REPORT);
		ValidationReport validationReport = (ValidationReport) context.get(MAIN_VALIDATION_REPORT);
		
		report.addFileInfo(filenameInfo, FILE_STATE.OK);
		validationReport.findCheckPointByName(checkpointName).setState(CheckPoint.RESULT.OK);
	}

	public void reportFailure(Context context, String checkpointName, String filenameInfo) throws Exception {
		ActionReport report = (ActionReport) context.get(REPORT);
		ValidationReport validationReport = (ValidationReport) context.get(MAIN_VALIDATION_REPORT);
		
		report.addFileInfo(filenameInfo, FILE_STATE.ERROR,
				new FileError(FileError.CODE.FILE_NOT_FOUND, "The file \""+filenameInfo+"\" must be provided (rule "+checkpointName+")"));
		validationReport.addDetail(checkpointName,
				new Location(filenameInfo, name(filenameInfo)+"-failure"),
				"The file \""+filenameInfo+"\" must be provided",
				CheckPoint.RESULT.NOK);
		// Stop parsing and render reports (1-GTFS-Agency-1 is fatal)
		throw new Exception("The file \"+GTFS_AGENCY_FILE+\" must be provided");
	}
}