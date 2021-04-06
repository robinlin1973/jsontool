package com.melax.json2xmi;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.uima.UIMAException;
import org.apache.uima.jcas.tcas.Annotation;

import edu.uth.clamp.nlp.structure.ClampNameEntity;
import edu.uth.clamp.nlp.structure.ClampRelation;
import edu.uth.clamp.nlp.structure.ClampSentence;
import edu.uth.clamp.nlp.structure.ClampToken;
import edu.uth.clamp.nlp.structure.Document;
import edu.uth.clamp.nlp.typesystem.ClampNameEntityUIMA;

public class XmiToEntityBIOMain {
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

	public static String DocumentToBIOString(Document doc, Set<String> sematics) {
		StringBuilder sb = new StringBuilder();
		
		// cleanup the documents first;
		for (ClampRelation rel : doc.getRelations()) {
			rel.clear();
		}
		for (ClampNameEntity cne : doc.getNameEntity()) {
			if( sematics.isEmpty() ) {
				// if no filtering, then keep all entities;
				continue;
			} else if ( sematics.contains(cne.getSemanticTag())) {
				continue;
			} else {
				cne.clear();
			}
		}
		
		// Check Overlapping entities;
		Map<Annotation, Annotation> tokenEntityMap = new HashMap<Annotation, Annotation>();
		for (ClampNameEntity cne : doc.getNameEntity()) {
			for (ClampToken token : cne.getTokens()) {
				if( tokenEntityMap.containsKey( token.getUimaEnt() ) ) {
					ClampNameEntityUIMA ent1 = (ClampNameEntityUIMA) tokenEntityMap.get( token.getUimaEnt() );
					ClampNameEntityUIMA ent2 = (ClampNameEntityUIMA) cne.getUimaEnt();
					System.out.print( "Overlap entities: " );
					System.out.print( "Ent1=[" + ent1.getBegin() + " " + ent1.getEnd() + " " + ent1.getSemanticTag() + " " + ent1.getCoveredText().replace( "\r", " " ).replace( "\n", " " ) );
					System.out.print( " " );
					System.out.print( "Ent2=[" + ent2.getBegin() + " " + ent2.getEnd() + " " + ent2.getSemanticTag() + " " + ent2.getCoveredText().replace( "\r", " " ).replace( "\n", " " ) );
					System.out.println( "" );
				} else {
					tokenEntityMap.putIfAbsent( token.getUimaEnt(), cne.getUimaEnt() );					
				}
			}
		}

		// create the BIO string;
		Map<Annotation, String> tokenBIOMap = new HashMap<Annotation, String>();
		for (ClampNameEntity cne : doc.getNameEntity()) {
			int i = 0;
			for (ClampToken token : cne.getTokens()) {
				if (i == 0) {
					tokenBIOMap.put(token.getUimaEnt(), LABELB + cne.getSemanticTag());
				} else {
					tokenBIOMap.put(token.getUimaEnt(), LABELI + cne.getSemanticTag());
				}
				i += 1;
			}
		}

		for (ClampSentence sent : doc.getCachedSentences()) {
			for (ClampToken token : sent.getTokens()) {
				Annotation key = token.getUimaEnt();
				String tokenStr = token.textStr();
				String bio = LABELO;
				if (tokenBIOMap.containsKey(key)) {
					bio = tokenBIOMap.get(key);
				}
				sb.append(tokenStr);
				sb.append("\t");
				sb.append(bio);
				sb.append("\n");
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	public static void main(String[] argv) throws UIMAException, IOException {
		if( argv.length != 2 && argv.length != 3 ) {
			System.out.println( "Usage: <infile|dir> <outfile|dir> <semantic types (seperated by ',')>" );
			System.exit( -1 );
		}
		File indir = new File(argv[0]);
		File outdir = new File(argv[1]);
		if (!indir.exists()) {
			System.out.println("indir doesn't exist indir=[" + indir + "]");
			return;
		}
		Set<String> semSet = new HashSet<String>();

		if( argv.length == 3 ) {
			String sem = argv[2];			
			for (String s : sem.split(",")) {
				semSet.add(s.trim());
			}
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
