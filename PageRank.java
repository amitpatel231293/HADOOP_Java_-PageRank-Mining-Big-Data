import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Scanner;
import java.util.Vector;


public class PageRank {
	
	public static boolean DEBUG = false; 
	
	public static double TAX_RATE = 0.2;
	
	public static Vector<Boolean> nodeList;
	public static Vector<Vector<Integer>> linkList;
	public static int[] linkCount;
	public static int maxNode;
	public static int usedNodes;
	public static double[] distVector; 
	
	// Read the web definition file
	public static void ReadWebFile(String filename) {
		// initialize our output variables
		nodeList = new Vector<Boolean>(1000000);
		linkList = new Vector<Vector<Integer>>(1000000);
		
		// setup io
		Path path = Paths.get(filename);
		Scanner scanner;
		try {
			scanner =  new Scanner(path);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		// read file
		maxNode = -1;
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if (line.length() == 0) continue;
			if (line.startsWith("#")) continue;
			// split on non-digits and parse
			String[] values = line.toString().split("\\D+");
			int thisNode = Integer.parseInt(values[0]);
			int thatNode = Integer.parseInt(values[1]);
			// grow list if required
			if (thatNode > maxNode) {
				maxNode = thatNode;
				nodeList.setSize(maxNode+1);
				linkList.setSize(maxNode+1);
			}
			if (thisNode > maxNode) {
				maxNode = thisNode;
				nodeList.setSize(maxNode+1);
				linkList.setSize(maxNode+1);
			}
			// set that this node is used
			if (nodeList.get(thisNode) == null) {
				nodeList.set(thisNode, new Boolean(false));
			}
			if (!nodeList.get(thisNode)) {
				// create adjacency list for this entry
				linkList.set(thisNode, new Vector<Integer>());
				// and mark this node as used
				nodeList.set(thisNode, true);
			}
			// add link
			linkList.get(thisNode).add(thatNode);
		}
		// debug output to show that it's working!
		if (DEBUG) {
			System.out.println("Highest node # found is " + maxNode);
			for (Integer i : linkList.get(0)) {
				System.out.println("0 - " + i);
			}
		}
		// finished
		scanner.close();
	}
	
	
	// initialise population vectors and count number of outgoing links for each node
	public static void InitialiseState() {
		// create a list of the number of outgoing links from each used node
		// also count used nodes, so we can initialize the vector and construct the taxation vector
		usedNodes = 0;
		linkCount = new int[maxNode+1];
		for (int n = 0; n <= maxNode; n++) {
			// if this node is unused, skip it
			if (nodeList.get(n) == null) {
				continue;
			}
			linkCount[n] = linkList.get(n).size();
			usedNodes++;
		}
		// Initialise the state vector
		distVector = new double[maxNode+1];
		for (int n = 0; n <= maxNode; n++) {
			// if this node is unused, skip it
			if (nodeList.get(n) == null) {
				continue;
			}
			distVector[n] = 1.0/usedNodes;
			//distVector[n] = 1.0;
		}
	}

	// do a single iteration of updating the distVector by multiplying it with the sparse matrix
	// linkList/linkCount, and then applying the taxRate
	public static void sparseMultiply() {
		double[] newDistVector = new double[maxNode+1];
		// do the sparse multiply
		for (int n = 0; n <= maxNode; n++) {
			// if this node is unused, skip it
			if (nodeList.get(n) == null) {
				continue;
			}
			// work out how much to weight each outgoing link from this node
			double linkWeight = distVector[n]/linkCount[n];
			for (Integer n2 : linkList.get(n)) {
				newDistVector[n2] += linkWeight;
			}
		}
		// apply tax rate as we copy to original dist vector
		for (int n = 0; n <= maxNode; n++) {
			//distVector[n] = (1-TAX_RATE)*newDistVector[n] + TAX_RATE;
			distVector[n] = (1-TAX_RATE)*newDistVector[n] + TAX_RATE/usedNodes;
		}
	}
	
	
	
	public static void writePageRank(String outName) {
		Path path = Paths.get(outName);
		//DecimalFormat df = new DecimalFormat(".####");
		DecimalFormat df = new DecimalFormat(".########");
		try (BufferedWriter writer = Files.newBufferedWriter(path, Charset.defaultCharset())){
			for (int n = 0; n <= maxNode; n++) {
				// if this node is unused, skip it
				if (nodeList.get(n) == null) {
					continue;
				}
				writer.write(n + "  " + df.format(distVector[n]));
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	public static void sortPageRank(String top10File) {
		// sort indices using custom Comparator
		Integer[] indices = new Integer[maxNode+1];
		for (int i = 0; i < indices.length; i++) {
		    indices[i] = i;
		}
		Comparator<Integer> comparator = new Comparator<Integer>() {
		    @Override
		    public int compare(Integer arg0, Integer arg1) {
		        return -Double.compare(distVector[arg0], distVector[arg1]);
		    }
		};
		Arrays.sort(indices, comparator);
		// show them to check that it's worked
		//DecimalFormat df = new DecimalFormat(".####");
		DecimalFormat df = new DecimalFormat(".00000000");
		for (int i = 0; i < 10; i++) {
			System.out.println(new DecimalFormat("00").format((i+1)) + "  " + new DecimalFormat("000000").format(indices[i]) + " " + df.format(distVector[indices[i]]));
		}
		// save them to file
		Path path = Paths.get(top10File);
		try (BufferedWriter writer = Files.newBufferedWriter(path, Charset.defaultCharset())){
			writer.write(" i  node#   PageRank");
			writer.newLine();
			for (int i = 0; i < 10; i++) {
				writer.write(new DecimalFormat("00").format((i+1)) + "  " + new DecimalFormat("000000").format(indices[i]) + " " + df.format(distVector[indices[i]]));
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	
	public static void main(String[] args) throws IOException {
		long totalStart = System.nanoTime();

		// set up defaults then parse arguments
		String inputFile = "/home/cloudera/web-Google.txt";
		String pageRankFile = "/home/cloudera/PageRank.txt";
		String top10File = "/home/cloudera/Top10PageRank.txt";
		double beta = 0.8;
		if (args.length > 0) inputFile = args[0];
		if (args.length > 1) pageRankFile = args[1];
		if (args.length > 2) top10File = args[2];
		if (args.length > 3) beta = Double.parseDouble(args[3]);
		TAX_RATE = 1.0 - beta;
		
		// Read the file
		long readStart = System.nanoTime();
		System.out.println("Reading file ...");
		ReadWebFile(inputFile);
		System.out.println("Reading took " + (System.nanoTime()-readStart)/1000000000.0);
		System.out.println("File read, now doing 100 iterations of sparse multiply");
		
		// Perform 100 iterations of sparse multiply + taxation
		// note that this takes ~20 seconds, so we just do it for a fixed number of iterations rather than checking convergence
		long multStart = System.nanoTime();
		InitialiseState();
		for (int i = 0; i <100; i++) {
			sparseMultiply();
		}
		System.out.println("PageRank took " + (System.nanoTime()-multStart)/1000000000.0);
		
		// Write results to a file
		long writeStart = System.nanoTime();
		System.out.println("Done. writing PageRank.txt");
		writePageRank(pageRankFile);
		
		// do one more iteration and write to a validation file. This can be manually compared to check convergence
		sparseMultiply();
		writePageRank(pageRankFile + ".validation");
		
		// Find the top 10 pages and write them to a file
		System.out.println("Done. Finding top 10");
		sortPageRank(top10File);
		System.out.println("Finished. Sorting and Writing took " + (System.nanoTime()-writeStart)/1000000000.0);
		
		System.out.println("Total time taken " + (System.nanoTime()-totalStart)/1000000000.0);

	}

}
