import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ConsoleReader implements Runnable {

	NodeServer nodeServ;

	public ConsoleReader(NodeServer nodeServ) {
		this.nodeServ = nodeServ;
	}

	public void run() {
		// The console reader accepts incoming commands from the user through
		// the console in a separate thread.

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

		String input = "";
		Boolean flag = true;

		try {
			while ((input.equalsIgnoreCase("quit") == false) && (flag)) {

				// take input from user
				input = in.readLine();

				System.out.println();

				if (input.equalsIgnoreCase("neighbours")) {
					nodeServ.printNeighbours(nodeServ);
				} else if (input.equalsIgnoreCase("supernode")) {
					nodeServ.printSuperNode(nodeServ);
				} else if (input.equalsIgnoreCase("files")) {
					nodeServ.printFilenames(nodeServ);
				} else if (input.equalsIgnoreCase("childnodes")) {
					nodeServ.printChildNodes(nodeServ);
				} 
				else if (input.equalsIgnoreCase("show")) {
					nodeServ.printMessages(nodeServ);
				}
				else if (input.equalsIgnoreCase("leave")) {
					nodeServ.leaveDS(nodeServ);
					break;
				} else {
					nodeServ.searchQuery(nodeServ, input.trim());
				}
				System.out.println();
			}
			in.close();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

	}

}
