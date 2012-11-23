package de.typology.nGramBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import de.typology.utils.IOHelper;
import de.typology.utils.Parallelizer;

public class Aggregator {
	Aggregator() {
	}

	public boolean aggregateNGrams(String sourcePath, String fileExtension,
			int numberOfThreads) {

		File[] files = IOHelper.getAllFilesInDirWithExtension(sourcePath,
				fileExtension, this.getClass().getName() + "aggregateNGrams");

		for (File f : files) {
			String fileName = f.getName();
			if (!fileName.endsWith(fileExtension)) {
				IOHelper.log(fileName
						+ " is not an un aggregated ngram file. process next");
				continue;
			}
			System.out.println(sourcePath + File.separator + fileName);

			String fullQualifiedFileName = sourcePath + File.separator
					+ fileName;
			IOHelper.strongLog("\t\t\tstart thread to aggregate: "
					+ fullQualifiedFileName);

			String line = "";
			int lCnt = 0;

			String aggregatedFileExtension = fileExtension.replace("c", "a");

			String outFileName = fullQualifiedFileName.replaceFirst(
					fileExtension, aggregatedFileExtension);
			BufferedWriter bw = IOHelper.openWriteFile(outFileName,
					32 * 1024 * 1024);
			int nCnt = 0;

			int div = 4;// yes this loop is neccessary du to sucking java can
						// only allocate 2GB per thread in unix and I wanted to
						// have a dirty solution.
			try {

				for (int mod = 0; mod < div; mod++) {
					HashMap<String, Integer> nGrams = new HashMap<String, Integer>();
					BufferedReader br = IOHelper.openReadFile(
							fullQualifiedFileName, 32 * 1024 * 1024);
					while ((line = br.readLine()) != null) {
						String[] values = line.split("\t#");
						if (values.length != 2) {
							IOHelper.log("bad format for: " + line
									+ " in file: " + fullQualifiedFileName);
							continue;
						}
						String key = values[0];
						Integer value = Integer.parseInt(values[1]);

						if (key.length() % div == mod) {
							Integer cnt = nGrams.get(key);
							lCnt++;
							if (cnt != null) {
								nGrams.put(key, cnt + value);
							} else {
								nGrams.put(key, value);
							}
							if (lCnt % 5000000 == 0) {
								IOHelper.log(fullQualifiedFileName + "\t"
										+ lCnt
										+ " nGrams processed for aggregating");
							}
						} else {
							continue;
						}
					}

					IOHelper.log("aggregation done for: "
							+ fullQualifiedFileName + "\nstart writing to file");
					for (String nGram : nGrams.keySet()) {
						bw.write(nGram + "\t#" + nGrams.get(nGram) + "\n");
						nCnt++;
						if (nCnt % 1000000 == 0) {
							bw.flush();
							IOHelper.log(fullQualifiedFileName + "\t" + nCnt
									+ " written to file");
						}
					}
					bw.flush();

					br.close();
					// TODO: comment in the following line to DELETE THE
					// UNAGGREGATED FILE AND SAVE DISKSPACE
				}
				bw.close();
				f.delete();

			} catch (IOException e) {
				e.printStackTrace();
			}
			IOHelper.strongLog("\t\t\tsuccessfully aggregated: "
					+ fullQualifiedFileName);
		}
		return true;
	}

	public boolean aggregateNGramsParallelVersion(String sourcePath,
			final String fileExtension, int numberOfThreads) {

		File[] files = IOHelper.getAllFilesInDirWithExtension(sourcePath,
				fileExtension, this.getClass().getName() + "aggregateNGrams");

		Parallelizer pll = new Parallelizer(numberOfThreads);
		for (File f : files) {
			String fileName = f.getName();
			if (!fileName.endsWith(fileExtension)) {
				IOHelper.log(fileName
						+ " is not an un aggregated ngram file. process next");
				continue;
			}
			System.out.println(sourcePath + File.separator + fileName);

			final String fullQualifiedFileName = sourcePath + File.separator
					+ fileName;
			pll.run(new Runnable() {
				@Override
				public void run() {
					IOHelper.strongLog("\t\t\tstart thread to aggregate: "
							+ fullQualifiedFileName);
					BufferedReader br = IOHelper.openReadFile(
							fullQualifiedFileName, 32 * 1024 * 1024);
					HashMap<String, Integer> nGrams = new HashMap<String, Integer>();
					String line = "";
					int lCnt = 0;
					try {
						while ((line = br.readLine()) != null) {
							String[] values = line.split("\t#");
							if (values.length != 2) {
								IOHelper.log("bad format for: " + line
										+ " in file: " + fullQualifiedFileName);
								continue;
							}
							String key = values[0];
							Integer value = Integer.parseInt(values[1]);
							Integer cnt = nGrams.get(key);
							lCnt++;
							if (cnt != null) {
								nGrams.put(key, cnt + value);
							} else {
								nGrams.put(key, value);
							}
							if (lCnt % 5000000 == 0) {
								IOHelper.log(fullQualifiedFileName + "\t"
										+ lCnt
										+ " nGrams processed for aggregating");
							}
						}
						IOHelper.log("aggregation done for: "
								+ fullQualifiedFileName
								+ "\nstart writing to file");
						String outFileName = fullQualifiedFileName
								.replaceFirst(fileExtension, ".ag");
						BufferedWriter bw = IOHelper.openWriteFile(outFileName,
								32 * 1024 * 1024);
						int nCnt = 0;
						for (String nGram : nGrams.keySet()) {
							bw.write(nGram + "\t#" + nGrams.get(nGram) + "\n");
							nCnt++;
							if (nCnt % 1000000 == 0) {
								bw.flush();
								IOHelper.log(fullQualifiedFileName + "\t"
										+ nCnt + " written to file");
							}
						}
						bw.flush();
						bw.close();
						br.close();
						// DELETE THE UNAGGREGATED FILE TO SAVE DISKSPACE
						File f = new File(fullQualifiedFileName);
						// TODO: comment in! f.delete();
					} catch (IOException e) {
						e.printStackTrace();
					}
					IOHelper.strongLog("\t\t\tsuccessfully aggregated: "
							+ fullQualifiedFileName);
				}
			});
		}

		try {
			pll.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
}