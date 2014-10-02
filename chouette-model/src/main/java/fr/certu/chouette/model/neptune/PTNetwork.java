/**
 * Projet CHOUETTE
 *
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 *
 */
package fr.certu.chouette.model.neptune;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import fr.certu.chouette.model.neptune.type.PTNetworkSourceTypeEnum;

/**
 * Neptune Public Transport Network
 * <p/>
 * Note for fields comment : <br/>
 * when readable is added to comment, a implicit getter is available <br/>
 * when writable is added to comment, a implicit setter is available
 */
@Entity
@Table(name = "networks")
@NoArgsConstructor
@Log4j
public class PTNetwork extends NeptuneIdentifiedObject
{

   private static final long serialVersionUID = -8986371268064619423L;

   /**
    * name of comment attribute for {@link Filter} attributeName construction
    */
   public static final String COMMENT = "comment";

   @Getter
   @Column(name = "name")
   private String name;

   @Getter
   @Column(name = "comment")
   private String comment;

   @Getter
   @Setter
   @Temporal(TemporalType.DATE)
   @Column(name = "version_date")
   private Date versionDate;

   @Getter
   @Setter
   @Column(name = "description")
   private String description;

   @Getter
   @Column(name = "registration_number")
   private String registrationNumber;

   @Getter
   @Setter
   @Enumerated(EnumType.STRING)
   @Column(name = "source_type")
   private PTNetworkSourceTypeEnum sourceType;

   @Getter
   @Setter
   @Column(name = "source_name")
   private String sourceName;

   @Getter
   @Setter
   @Column(name = "source_identifier")
   private String sourceIdentifier;

   @Getter
   @Setter
   @OneToMany(mappedBy = "ptNetwork")
   private List<Line> lines = new ArrayList<Line>(0);

   /**
    * List of the network lines Neptune Ids<br/>
    * After import, may content only lines imported<br/>
    * Meaningless after database loading <br/>
    * <i>readable/writable</i>
    */
   @Getter
   @Setter
   @Transient
   private List<String> lineIds;

   public void setRegistrationNumber(String value)
   {
      if (value != null && value.length() > 255)
      {
         log.warn("registrationNumber too long, truncated " + value);
         registrationNumber = value.substring(0, 255);
      } else
      {
         registrationNumber = value;
      }
   }

   public void setName(String value)
   {
      if (value != null && value.length() > 255)
      {
         log.warn("name too long, truncated " + value);
         name = value.substring(0, 255);
      } else
      {
         name = value;
      }
   }

   public void setComment(String value)
   {
      if (value != null && value.length() > 255)
      {
         log.warn("comment too long, truncated " + value);
         comment = value.substring(0, 255);
      } else
      {
         comment = value;
      }
   }

   @Override
   public String toString(String indent, int level)
   {
      SimpleDateFormat f = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
      StringBuilder sb = new StringBuilder(super.toString(indent, level));
      if (versionDate != null)
         sb.append("\n").append(indent).append("versionDate = ")
               .append(f.format(versionDate));
      sb.append("\n").append(indent).append("description = ")
            .append(description);
      sb.append("\n").append(indent).append("registrationNumber = ")
            .append(registrationNumber);
      sb.append("\n").append(indent).append("sourceName = ").append(sourceName);
      sb.append("\n").append(indent).append("sourceIdentifier = ")
            .append(sourceIdentifier);
      sb.append("\n").append(indent).append("comment = ").append(comment);

      if (lineIds != null)
      {
         sb.append("\n").append(indent).append(CHILD_ARROW).append("lineIds");
         for (String lineId : lineIds)
         {
            sb.append("\n").append(indent).append(CHILD_LIST_ARROW)
                  .append(lineId);
         }
      }

      return sb.toString();
   }

   /**
    * add a line Id to the network
    * 
    * @param lineId
    *           the line id to add
    */
   public void addLineId(String lineId)
   {
      if (lineIds == null)
         lineIds = new ArrayList<String>();
      lineIds.add(lineId);
   }

   /**
    * add a line Id to the network
    * 
    * @param line
    *           the line to add
    */
   public void addLine(Line line)
   {
      if (lines == null)
         lines = new ArrayList<Line>();
      if (!lines.contains(line))
         lines.add(line);
   }

   /**
    * remove a line
    * 
    * @param line
    *           the lien to remove
    */
   public void removeLine(Line line)
   {
      if (lines == null)
         lines = new ArrayList<Line>();
      if (lines.contains(line))
         lines.remove(line);
   }

   @Override
   public <T extends NeptuneObject> boolean compareAttributes(T anotherObject)
   {
      if (anotherObject instanceof PTNetwork)
      {
         PTNetwork another = (PTNetwork) anotherObject;
         if (!sameValue(this.getObjectId(), another.getObjectId()))
            return false;
         if (!sameValue(this.getObjectVersion(), another.getObjectVersion()))
            return false;
         if (!sameValue(this.getName(), another.getName()))
            return false;
         if (!sameValue(this.getComment(), another.getComment()))
            return false;
         if (!sameValue(this.getRegistrationNumber(),
               another.getRegistrationNumber()))
            return false;

         if (!sameValue(this.getDescription(), another.getDescription()))
            return false;
         if (!sameValue(this.getSourceIdentifier(),
               another.getSourceIdentifier()))
            return false;
         if (!sameValue(this.getSourceName(), another.getSourceName()))
            return false;
         if (!sameValue(this.getSourceType(), another.getSourceType()))
            return false;
         if (!sameValue(this.getVersionDate(), another.getVersionDate()))
            return false;
         return true;
      } else
      {
         return false;
      }
   }

   @Override
   public String toURL()
   {
      return "networks/" + getId();
   }

}