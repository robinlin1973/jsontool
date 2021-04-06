/* This file is licensed to you under the CLAMP Research and 
 * Evaluation Academic Use License Agreement (the "License"); 
 * you may not use this file except in compliance with the License.
 * See the NOTICE file distributed with this work for additional 
 * information regarding copyright ownership.
 * 
 * Software distributed under the License is distributed on an 
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied.  See the License for the specific 
 * language governing permissions and limitations under the License.
 */

package com.melax.json2xmi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.FileUtils;
import org.xml.sax.SAXException;

import edu.uth.clamp.io.DocumentIOException;
import edu.uth.clamp.nlp.core.ClampSentDetector;
import edu.uth.clamp.nlp.core.ClampTokenizer;
import edu.uth.clamp.nlp.core.OpenNLPPosTagger;
import edu.uth.clamp.nlp.core.PosTaggerUIMA;
import edu.uth.clamp.nlp.core.SentDetectorUIMA;
import edu.uth.clamp.nlp.core.TokenizerUIMA;
import edu.uth.clamp.nlp.encoding.UmlsIndexBuilder;
import edu.uth.clamp.nlp.structure.ClampNameEntity;
import edu.uth.clamp.nlp.structure.ClampRelation;
import edu.uth.clamp.nlp.structure.DocProcessor;
import edu.uth.clamp.nlp.structure.Document;


/**
 *
 *
 * @author Jingqi Wang
 *
 */
public class BratToXmi {

	static Logger log = LoggerFactory.getLogger( BratToXmi.class.getName() );
	
    private static ClampNameEntity getEntityById( Map<String, ClampNameEntity> entityMap, 
                                Map<String, String> eventToEntityMap, 
                                String eventId ) {
        String entityId = eventId;
        while( true ) {
            if( entityMap.containsKey( entityId ) ) {
                return entityMap.get( entityId );
            } else if( eventToEntityMap.containsKey( entityId ) ) {
                if( entityId.equals( eventToEntityMap.get( entityId ) ) ) {
                    break;
                }
                entityId = eventToEntityMap.get( entityId );
            } else {
                break;
            }
        }
        return null;
    }
    
    public static Document parseBratFile(File txtFile, File annFile) throws IOException, UIMAException {
        log.trace( "textFile=[" + txtFile + "], annFile=[" + annFile + "]" );
        // 1. load file content;
        Document doc = new Document(txtFile);
        
        DocProcessor sentUIMA = new SentDetectorUIMA( ClampSentDetector.getDefault() );
        DocProcessor tokenUIMA = new TokenizerUIMA( ClampTokenizer.getDefault() );
        sentUIMA.process( doc );
        tokenUIMA.process( doc );
        

        // 2. load ann;
        String fileContent = FileUtils.file2String(annFile, "UTF-8");
        Map<String, ClampNameEntity> entityMap = new HashMap<String, ClampNameEntity>();
        Map<String, List<String>> relationMap = new HashMap<String, List<String>>();

        // 3. parse entities;
        for (String line : fileContent.split("\\n")) {
          if( !line.startsWith( "T" ) ) {
            continue;
          }
          String[] splitStr = line.trim().split("\t");
          if( splitStr.length != 3 ) {
            log.warn( "column count are wrong, line ignored. line=[" + line + "]" );
            continue;
          }
          String entityId = splitStr[0];
          String semantic = splitStr[1];
          if( semantic.indexOf( ";" ) >= 0 ) {
            log.warn( "disjoint entities, line ignored. line=[" + line + "]" );
            continue;
          }
          //String entity = splitStr[2];
          String startStr = semantic.split( "\\s" )[1];
          String endStr = semantic.split( "\\s" )[2];
          semantic = semantic.split( "\\s" )[0];
          int start = Integer.parseInt(startStr);
          int end = Integer.parseInt(endStr);
          ClampNameEntity cne = new ClampNameEntity(doc.getJCas(), start,
              end, semantic);
          cne.addToIndexes();
          entityMap.put(entityId, cne);      
        }

        // 4. parse relation;
        for (String line : fileContent.split("\\n")) {
          if( !line.startsWith( "R" ) ) {
            continue;
          }
          if( line.indexOf( "\t" ) < 0 ) {
            continue;
          }
          // parseRelations
          String[] splitStr = line.trim().split("\\s");
          String semantic = line.trim().split( "\\s" )[1];
          String fromId = splitStr[2].split(":")[1];
          String toId = semantic + ":" + splitStr[3].split(":")[1];
          if (!relationMap.containsKey(fromId)) {
            relationMap.put(fromId, new ArrayList<String>());
          }
          relationMap.get(fromId).add(toId);
        }

        // 5. create relations;
        for (String fromId : relationMap.keySet()) {
          ClampNameEntity fromNE = entityMap.get( fromId );
          if( fromNE == null ) {
            log.warn( "cannot find relation from entity, fromId=[" + fromId + "]");
            continue;
          }
          for( String toId : relationMap.get( fromId ) ) {
            int pos = toId.lastIndexOf( ":" );
            String semantic = toId.substring(0, pos);
            toId = toId.substring( pos + 1 );
            ClampNameEntity toNE = entityMap.get( toId );
            if( toNE == null ) {
              log.warn("cannot find relation from entity, toId=[" + toId + "]");
              continue;
            }        
            ClampRelation relation = new ClampRelation( fromNE, toNE, semantic );
            relation.addToIndexes();
          }
        }
        
        for( ClampRelation rel : doc.getRelations() ) {
            if( rel.getEntTo().textStr().toLowerCase().equals( "acute" ) ) {
                    System.out.println( rel.getSemanticTag() + "... to ... course" );
                    rel.getEntTo().setSemanticTag( "course" );
            }
        }
        
        return doc;
      }
        

    public static void main( String[] argv ) throws AnalysisEngineProcessException, IOException {
        String bratDirStr = argv[0];
    	System.out.println("bratDirStr: " + bratDirStr);
    	String xmiDirStr = argv[1];
    	System.out.println("xmiDirStr: " + xmiDirStr);

        File bratDir = new File( bratDirStr );
        
        for( File annFile : bratDir.listFiles() ) {
            if( annFile.getName().startsWith( "." ) ) {
                continue;
            } else if( !annFile.getName().endsWith( ".ann" ) ) {
                continue;
            }
            String txtFileName = annFile.getAbsolutePath();
            assert( txtFileName.endsWith( ".ann" ) );
            txtFileName = txtFileName.substring(0, txtFileName.length() - 4 );
            txtFileName += ".txt";
            File txtFile = new File( txtFileName );
            
            assert( annFile.exists() );
            assert( txtFile.exists() );

            Document doc;
			try {
				doc = BratToXmi.parseBratFile( txtFile, annFile );

            if( doc != null ) {
//				doc.save( txtFile.getAbsolutePath().replace(".txt", ".xmi" ) );
            	doc.save( xmiDirStr +"\\"+ txtFile.getName().replace(".txt", ".xmi" ) );
            }
			} catch (UIMAException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (DocumentIOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }

        return;
    }
}
