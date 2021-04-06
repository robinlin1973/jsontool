package com.melax.json2xmi;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.UIMAException;
import org.apache.uima.jcas.tcas.Annotation;

import edu.uth.clamp.nlp.structure.ClampNameEntity;
import edu.uth.clamp.nlp.structure.ClampRelation;
import edu.uth.clamp.nlp.structure.ClampSentence;
import edu.uth.clamp.nlp.structure.ClampToken;
import edu.uth.clamp.nlp.structure.Document;
import edu.uth.clamp.nlp.structure.XmiUtil;

public class XmiToRelationBIOMain {
	final static String LABELB = "B-";
	final static String LABELI = "I-";
	final static String LABELO = "O";
	
	public static void DirToDir(File indir, File outdir, Set<String> semantics) throws UIMAException, IOException {
		for( File file : indir.listFiles() ) {
			if( file.getName().startsWith( "." ) ) {
				continue;
			} else if( !file.getName().endsWith( ".xmi" ) ) {
				continue;
			}
			Document doc = new Document( file );
			String bio = DocumentToBIOString( doc, semantics );
			
			FileWriter outfile = new FileWriter( new File( outdir + "/" + file.getName().replace( ".xmi", ".bio" ) ) );
			outfile.write( bio );
			outfile.close();
			
			System.out.println( file.getName() + " processed... " );
		}
	}

	public static void DirToFile(File indir, File outfile, Set<String> semantics) throws UIMAException, IOException {
		FileWriter out = new FileWriter( outfile );
		for( File file : indir.listFiles() ) {
			if( file.getName().startsWith( "." ) ) {
				continue;
			} else if( !file.getName().endsWith( ".xmi" ) ) {
				continue;
			}
			Document doc = new Document( file );
			String bio = DocumentToBIOString( doc, semantics );
			out.write( bio );
			System.out.println( file.getName() + " processed... " );
		}
		out.close();

	}

	public static void FileToFile(File infile, File outfile, Set<String> semantics) throws UIMAException, IOException {
		if( infile.getName().startsWith( "." ) ) {
			System.out.println( "infile name started with '.' [" + infile.getName() + "]" ); 
			return;
		} else if( !infile.getName().endsWith( ".xmi" ) ) {
			System.out.println( "infile is not an xmi file. [" + infile.getName() + "]" ); 
			return;
		}
		Document doc = new Document( infile );
		String bio = DocumentToBIOString( doc, semantics );
		FileWriter out = new FileWriter( outfile );
		out.write( bio );
		out.close();
		System.out.println( infile.getName() + " processed... " );
	}

	public static String DocumentToBIOString(Document doc, Set<String> semantics) {
		StringBuilder sb = new StringBuilder();
		Set<Annotation> entitySet = new HashSet<Annotation>();

		// cleanup the documents first;
		for (ClampRelation rel : doc.getRelations()) {
			if( semantics.contains( rel.getEntFrom().getSemanticTag() ) ) {
//			String sem = rel.getSemanticTag();
//			if( semantics.contains( sem ) ) {
				entitySet.add( rel.getEntFrom().getUimaEnt() );
				entitySet.add( rel.getEntTo().getUimaEnt() );
				continue;
			}
			rel.clear();
		}
		for (ClampNameEntity cne : doc.getNameEntity()) {
			if (semantics.contains(cne.getSemanticTag())) {
				continue;
			} else if( entitySet.contains( cne.getUimaEnt() ) ) {
				continue;
			}
			cne.clear();
		}
		
		for( ClampSentence sent : doc.getCachedSentences() ) {
			List<String> tokens = sent.getCachedTokenInStr();
			for( ClampNameEntity cne : sent.getEntities() ) {
				if( semantics.contains( cne.getSemanticTag() ) ) {
					List<String> feas = getFeature( sent, cne, semantics );
					List<String> tags = getTag( sent, cne );
					
					for( int i = 0; i < tokens.size(); i++ ) {
						sb.append( tokens.get(i) );
						sb.append( "\t" );
						sb.append( feas.get(i) );
						sb.append( "\t" );
						sb.append( tags.get(i) );
						sb.append( "\n" );
					}
					sb.append( "\n" );					
				}
			}
		}

		return sb.toString();
	}

	private static List<String> getTag(ClampSentence sent, ClampNameEntity primary ) {
		Set<Annotation> entities = new HashSet<Annotation>();
		for( ClampRelation rel : XmiUtil.selectRelation( sent.getJCas(), sent.getBegin(), sent.getEnd() ) ) {
			if( rel.getEntFrom().getUimaEnt().equals( primary.getUimaEnt() ) ) {
				entities.add( rel.getEntTo().getUimaEnt() );
			}
		}
		Map<Annotation, String> tokenBIOMap = new HashMap<Annotation, String>();
		for( ClampNameEntity cne : sent.getEntities() ) {
			if( !entities.contains( cne.getUimaEnt() ) ) {
				continue;
			}
			int i = 0;
			String sem = cne.getSemanticTag();
			for (ClampToken token : cne.getTokens()) {
				if (i == 0) {
					tokenBIOMap.put(token.getUimaEnt(), LABELB + sem);
				} else {
					tokenBIOMap.put(token.getUimaEnt(), LABELI + sem);
				}
				i += 1;
			}
		}
	
		List<String> ret = new ArrayList<String>();
		for (ClampToken token : sent.getTokens()) {
			Annotation key = token.getUimaEnt();
			String bio = LABELO;
			if (tokenBIOMap.containsKey(key)) {
				bio = tokenBIOMap.get(key);
			}
			ret.add( bio );
		}
		return ret;
	}

	private static List<String> getFeature(ClampSentence sent,
			ClampNameEntity primary, Set<String> semantics) {
		Map<Annotation, String> tokenBIOMap = new HashMap<Annotation, String>();
		for( ClampNameEntity cne : sent.getEntities( semantics ) ) {
			int i = 0;
			String sem = cne.getSemanticTag();
			if( cne.getUimaEnt().equals( primary.getUimaEnt() ) ) {
				sem = "primary-" + sem;
			}
			for (ClampToken token : cne.getTokens()) {
				if (i == 0) {
					tokenBIOMap.put(token.getUimaEnt(), LABELB + sem);
				} else {
					tokenBIOMap.put(token.getUimaEnt(), LABELI + sem);
				}
				i += 1;
			}
		}
		List<String> ret = new ArrayList<String>();
		for (ClampToken token : sent.getTokens()) {
			Annotation key = token.getUimaEnt();
			String bio = LABELO;
			if (tokenBIOMap.containsKey(key)) {
				bio = tokenBIOMap.get(key);
			}
			ret.add( bio );
		}
		return ret;
	}

	public static void main(String[] argv) throws UIMAException, IOException {
		if( argv.length != 3 ) {
			System.out.println( "Usage: <infile|dir> <outfile|dir> <semantic types (seperated by ',')>" );
			System.exit( -1 );
		}
		File indir = new File(argv[0]);
		File outdir = new File(argv[1]);
		String sem = argv[2];
		if (!indir.exists()) {
			System.out.println("indir doesn't exist indir=[" + indir + "]");
			return;
		}

		Set<String> semSet = new HashSet<String>();
		for (String s : sem.split(",")) {
			semSet.add(s.trim());
		}
		
		if( outdir.exists() && outdir.isDirectory() ) {
			if( indir.isDirectory() ) {
				DirToDir( indir, outdir, semSet );
				return;
			}
		} else {
			if( indir.isDirectory() ) {
				DirToFile( indir, outdir, semSet );
				return;
			} else if( indir.isFile() ) {
				FileToFile( indir, outdir, semSet );
				return;
			}
		}
		
		System.out.println( "Wrong pattern. only <dir to dir>, <dir to file> <file to file>");		
		return;
	}

}
