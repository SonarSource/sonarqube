package org.sonar.plugins.cobertura.api;

import static java.util.Locale.ENGLISH;
import static org.sonar.api.utils.ParsingUtils.parseNumber;
import static org.sonar.api.utils.ParsingUtils.scaleValue;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.measures.PropertiesBuilder;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.StaxParser;
import org.sonar.api.utils.XmlParserException;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

/**
 * @since 2.4
 */
public abstract class AbstractCoberturaParser {
  public void parseReport(File xmlFile, final SensorContext context) {
    try {
      StaxParser parser = new StaxParser(new StaxParser.XmlStreamHandler() {

        public void stream(SMHierarchicCursor rootCursor) throws XMLStreamException {
          try {
            rootCursor.advance();
            collectPackageMeasures(rootCursor.descendantElementCursor("package"), context);
          } catch (ParseException e) {
            throw new XMLStreamException(e);
          }
        }
      });
      parser.parse(xmlFile);
    } catch (XMLStreamException e) {
      throw new XmlParserException(e);
    }
  }

  private void collectPackageMeasures(SMInputCursor pack, SensorContext context) throws ParseException, XMLStreamException {
    while (pack.getNext() != null) {
      Map<String, FileData> fileDataPerFilename = new HashMap<String, FileData>();
      collectFileMeasures(pack.descendantElementCursor("class"), fileDataPerFilename);
      for (FileData cci : fileDataPerFilename.values()) {
        if (isFileExist(context, cci.getFile())) {
          for (Measure measure : cci.getMeasures()) {
            context.saveMeasure(cci.getFile(), measure);
          }
        }
      }
    }
  }

  private boolean isFileExist(SensorContext context, Resource<?> file) {
    return context.getResource(file) != null;
  }

  private void collectFileMeasures(SMInputCursor clazz, Map<String, FileData> dataPerFilename) throws ParseException, XMLStreamException {
    while (clazz.getNext() != null) {
      String fileName = FilenameUtils.removeExtension(clazz.getAttrValue("filename"));
      fileName = fileName.replace('/', '.').replace('\\', '.');
      FileData data = dataPerFilename.get(fileName);
      if (data == null) {
        data = new FileData(getResource(fileName));
        dataPerFilename.put(fileName, data);
      }
      collectFileData(clazz, data);
    }
  }

  private void collectFileData(SMInputCursor clazz, FileData data) throws ParseException, XMLStreamException {
    SMInputCursor line = clazz.childElementCursor("lines").advance().childElementCursor("line");
    while (line.getNext() != null) {
      String lineId = line.getAttrValue("number");
      data.addLine(lineId, (int) parseNumber(line.getAttrValue("hits"), ENGLISH));

      String isBranch = line.getAttrValue("branch");
      String text = line.getAttrValue("condition-coverage");
      if (StringUtils.equals(isBranch, "true") && StringUtils.isNotBlank(text)) {
        String[] conditions = StringUtils.split(StringUtils.substringBetween(text, "(", ")"), "/");
        data.addConditionLine(lineId, Integer.parseInt(conditions[0]), Integer.parseInt(conditions[1]), StringUtils.substringBefore(text, " "));
      }
    }
  }

  private class FileData {

    private int lines = 0;
    private int conditions = 0;
    private int coveredLines = 0;
    private int coveredConditions = 0;

    private Resource<?> file;
    private PropertiesBuilder<String, Integer> lineHitsBuilder = new PropertiesBuilder<String, Integer>(CoreMetrics.COVERAGE_LINE_HITS_DATA);
    private PropertiesBuilder<String, String> branchHitsBuilder = new PropertiesBuilder<String, String>(CoreMetrics.BRANCH_COVERAGE_HITS_DATA);

    public void addLine(String lineId, int lineHits) {
      lines++;
      if (lineHits > 0) {
        coveredLines++;
      }
      lineHitsBuilder.add(lineId, lineHits);
    }

    public void addConditionLine(String lineId, int coveredConditions, int conditions, String label) {
      this.conditions += conditions;
      this.coveredConditions += coveredConditions;
      branchHitsBuilder.add(lineId, label);
    }

    public FileData(Resource<?> file) {
      this.file = file;
    }

    public List<Measure> getMeasures() {
      List<Measure> measures = new ArrayList<Measure>();
      if (lines > 0) {
        measures.add(new Measure(CoreMetrics.COVERAGE, calculateCoverage(coveredLines + coveredConditions, lines + conditions)));

        measures.add(new Measure(CoreMetrics.LINE_COVERAGE, calculateCoverage(coveredLines, lines)));
        measures.add(new Measure(CoreMetrics.LINES_TO_COVER, (double) lines));
        measures.add(new Measure(CoreMetrics.UNCOVERED_LINES, (double) lines - coveredLines));
        measures.add(lineHitsBuilder.build().setPersistenceMode(PersistenceMode.DATABASE));

        if (conditions > 0) {
          measures.add(new Measure(CoreMetrics.BRANCH_COVERAGE, calculateCoverage(coveredConditions, conditions)));
          measures.add(new Measure(CoreMetrics.CONDITIONS_TO_COVER, (double) conditions));
          measures.add(new Measure(CoreMetrics.UNCOVERED_CONDITIONS, (double) conditions - coveredConditions));
          measures.add(branchHitsBuilder.build().setPersistenceMode(PersistenceMode.DATABASE));
        }
      }
      return measures;
    }

    public Resource<?> getFile() {
      return file;
    }
  }

  private double calculateCoverage(int coveredElements, int elements) {
    if (elements > 0) {
      return scaleValue(100.0 * ((double) coveredElements / (double) elements));
    }
    return 0.0;
  }

  protected abstract Resource<?> getResource(String fileName);
}
