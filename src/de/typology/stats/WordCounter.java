package de.typology.stats;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class WordCounter {
	private static ArrayList<File> files;
	private BufferedReader reader;
	private BufferedWriter wordsWriter;
	private BufferedWriter statsWriter;
	private BufferedWriter wordsDistributionWriter;
	private HashMap<String, Integer> wordMap;
	private TreeMap<String, Integer> sortedWordMap;
	private String line;
	private long wordCount;
	private long wordCountCheck;

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		files = IOHelper.getFileList(new File(Config.get().wordCountInput));
		for (File file : files) {
			long startTime = System.currentTimeMillis();
			long endTime = 0;
			long sek = 0;
			String filePathCut = file.getAbsolutePath().substring(0,
					file.getAbsolutePath().length() - 4);

			WordCounter wC = new WordCounter(file.getAbsolutePath(),
					filePathCut + "words.txt", filePathCut
							+ "wordsdistribution.txt",
					Config.get().wordCountStats);
			System.out.println(file.getAbsolutePath() + ": ");
			System.out.println("start counting");
			wC.countWords();
			System.out.println("counting done, start sorting");
			wC.sortWordMap();
			System.out.println("sorting done, start writing to file");
			wC.printWordsAndDistribution();
			endTime = System.currentTimeMillis();
			sek = (endTime - startTime) / 1000;
			wC.printStats(file.getAbsolutePath(), sek);
			System.out.println("done");
		}
	}

	public WordCounter(String input, String wordsOutput,
			String wordsDistributionOutput, String statsOutput)
			throws IOException {
		this.reader = IOHelper.openReadFile(input);
		this.wordsWriter = IOHelper.openWriteFile(wordsOutput);
		this.wordsDistributionWriter = IOHelper
				.openWriteFile(wordsDistributionOutput);
		this.statsWriter = new BufferedWriter(new FileWriter(statsOutput, true));
	}

	private void printStats(String info, long sek) throws IOException {
		this.statsWriter.write(info + ":" + "\n");
		this.statsWriter.write("\t" + "total words: " + this.wordCount);
		if (this.wordCountCheck == this.wordCount) {
			this.statsWriter.write(" (checked)\n");
		} else {
			this.statsWriter.write(" (check failed: should be equal to:"
					+ this.wordCountCheck + ")\n");
		}
		this.statsWriter.write("\t" + "unique words: " + this.wordMap.size()
				+ "\n");
		this.statsWriter.write("\t" + "seconds to generate stats: " + sek
				+ "\n");
		Date date = new Date();
		this.statsWriter.write("\t" + "date: " + date + "\n");
		this.statsWriter.flush();
		this.statsWriter.close();
	}

	private final Comparator<String> StringComparator = new Comparator<String>() {
		@Override
		public int compare(String a, String b) {
			if (WordCounter.this.wordMap.get(a) >= WordCounter.this.wordMap
					.get(b)) {
				return -1;
			} else {
				return 1;
			} // returning 0 would merge keys
		}
	};

	public void countWords() throws IOException {
		this.wordMap = new HashMap<String, Integer>();

		while ((this.line = this.reader.readLine()) != null) {
			String[] words = this.line.split(" ");
			for (String word : words) {
				this.wordCount++;
				if (this.wordMap.containsKey(word)) {
					this.wordMap.put(word, this.wordMap.get(word) + 1);
				} else {
					this.wordMap.put(word, 1);
				}
			}
		}
		this.reader.close();
	}

	public void sortWordMap() {
		this.sortedWordMap = new TreeMap<String, Integer>(this.StringComparator);
		this.sortedWordMap.putAll(this.wordMap);
	}

	private void printWordsAndDistribution() throws IOException {
		Entry<String, Integer> entry;
		int currentOccurrences;
		int currentOccurrencesCount;
		// first entry has to be handled separately due to initializing
		// occurrences
		if ((entry = this.sortedWordMap.pollFirstEntry()) != null) {
			currentOccurrences = entry.getValue();
			currentOccurrencesCount = 1;
			this.wordsWriter.write(entry.getKey() + "#" + entry.getValue()
					+ "\n");
			this.wordsWriter.flush();
			this.wordCountCheck += entry.getValue();
			// for (int i = 0; i < 1000; i++) {
			while (true) {
				if ((entry = this.sortedWordMap.pollFirstEntry()) != null) {
					// map is not empty
					if (currentOccurrences == entry.getValue()) {
						currentOccurrencesCount++;
					} else {
						this.wordsDistributionWriter.write(currentOccurrences
								+ "#" + currentOccurrencesCount + "\n");
						this.wordsDistributionWriter.flush();
						currentOccurrences = entry.getValue();
						currentOccurrencesCount = 1;
					}
					this.wordCountCheck += entry.getValue();
					this.wordsWriter.write(entry.getKey() + "#"
							+ entry.getValue() + "\n");
					this.wordsWriter.flush();
				} else {
					// map is empty
					this.wordsDistributionWriter.write(currentOccurrences + "#"
							+ currentOccurrencesCount + "\n");
					this.wordsDistributionWriter.flush();
					break;
				}
			}
		}
		this.wordsWriter.close();
		this.wordsDistributionWriter.close();
	}
}