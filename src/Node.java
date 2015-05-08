import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class Node {

	public Boolean isSN = false;
	public String nodeIP;
	public Integer nodePort;
	public Integer num_files = 0;
	public ArrayList<String> filenames = new ArrayList<String>();
	public String superNodeIP;
	public Integer superNodePort;
	public Integer hopsSN = 0;

	public Node() {

	}

	public Node(String IP, Integer Port) {
		this.nodeIP = IP;
		this.nodePort = Port;
	}

	public void setFiles() {

		//Get list of filenames present in current node from file*.txt file
		BufferedReader br;
		try {

			File path = new File(".");

			File[] FilesList = path.listFiles();

			if (FilesList.length != 0) {
				for (int i = 0; i < FilesList.length; i++) {

					if ((FilesList[i].getName().contains("file")) && (FilesList[i].getName().contains(".txt"))) {
						br = new BufferedReader(new FileReader(path + "/" + FilesList[i].getName()));
						String line;
						while ((line = br.readLine()) != null) {
							filenames.add(line);
							System.out.println(line);
						}
						br.close();
						break;
					}

				}
			}

			num_files = filenames.size();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		//For local testing
		// Take files present in /tmp/koneru
		/*
		 * File path = new File("/tmp/koneru");
		 * 
		 * if (path.exists()) { File[] FilesList = path.listFiles();
		 * 
		 * System.out.println("Files in Current Node: " + nodeIP +":"+
		 * nodePort);
		 * 
		 * if (FilesList.length != 0) { for (int i = 0; i < FilesList.length;
		 * i++) { filenames.add(FilesList[i].getName());
		 * System.out.println(FilesList[i].getName()); } } num_files =
		 * FilesList.length; }
		 */
	}

}
