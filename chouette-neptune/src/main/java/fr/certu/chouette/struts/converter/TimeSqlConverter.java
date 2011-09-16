/*
 * $Id: DateConverter.java,v 1.7 2008-06-27 10:15:09 zakaria Exp $
 *
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.certu.chouette.struts.converter;

import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.util.StrutsTypeConverter;

import fr.certu.chouette.service.commun.CodeDetailIncident;
import fr.certu.chouette.service.commun.CodeIncident;
import fr.certu.chouette.service.commun.ServiceException;

/**
 * 
 */
public class TimeSqlConverter extends StrutsTypeConverter
{

	private static final Log				log			= LogFactory.getLog(TimeSqlConverter.class);

	private static final SimpleDateFormat	sdfHoraire	= new SimpleDateFormat("HH:mm");

	private static final Calendar			calendar	= Calendar.getInstance();

	public Object convertFromString(Map context, String[] values, Class toClass)
	{
		if (values != null && values.length > 0 && values[0] != null && !values[0].isEmpty())
		{
			final String dateString = values[0].toString();
			try
			{
				java.util.Date date =  sdfHoraire.parse(dateString);
				return new Time(date.getTime());
			}
			catch (ParseException e)
			{
				throw new ServiceException(CodeIncident.DONNEE_INVALIDE,CodeDetailIncident.DATETIME_TYPE, dateString );
			}
		}
		return null;
	}

	public String convertToString(Map context, Object o)
	{
		if (o instanceof Time)
		{
			Date date = (Date) o;
			calendar.setTime(date);
			return sdfHoraire.format(date);

		}
		return "";
	}
}
