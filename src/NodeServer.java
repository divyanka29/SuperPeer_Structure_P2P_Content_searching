import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.StringTokenizer;

public class NodeServer {

	// to set the count of when a node becomes a SuperNode
	Integer minNodesForSN = 0;

	Node curNode;
	ArrayList<Node> neighbourNodes = new ArrayList<Node>();
	Boolean isSN = false;
	SuperNode superNode;

	static DatagramSocket sock;

	Boolean unregOK = false;

	String queryStartTime;
	String queryEndTime;

	// To keep track of the queries
	Integer rcvdMsgs = 0;
	Integer frwdMsgs = 0;
	Integer answdMsgs = 0;

	Integer lastSendSNList = 0;
	Node lastsentSNListNode = null;
	Boolean sync = false;

	// Initialize the node with local addres and files present in specified
	// location
	public NodeServer() {

		try {
			curNode = new Node();

			curNode.nodeIP = InetAddress.getLocalHost().getHostAddress();

			sock = new DatagramSocket(0);
			curNode.nodePort = sock.getLocalPort();
			curNode.setFiles();
			// System.out.println("Current Node: " + curNode.nodeIP + ":" +
			// curNode.nodePort);

		} catch (SocketException | UnknownHostException e) {
			e.printStackTrace();
		}
	}

	// connection to Bootstrap server
	public void connect_to_bootstrap(NodeServer nodeServ) throws IOException {
		byte[] sendData;
		byte[] recvData = new byte[5000];
		try {

			// IP and Port of Bootstrap server
			InetAddress BS_IP = InetAddress.getByName("planetlab-1.cs.colostate.edu");
			// InetAddress BS_IP =
			// InetAddress.getByName("plink.cs.uwaterloo.ca");
			int BS_port = 10000;

			// Connection request
			String connect_to_bs_1 = " REG " + InetAddress.getLocalHost().getHostAddress() + " " + sock.getLocalPort() + " div_han";
			String connectBS = AuxFunctions.getMsg(connect_to_bs_1);

			// Send registration request to Bootstrap Server
			sendData = connectBS.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, BS_IP, BS_port);
			sock.send(sendPacket);

			System.out.println();
			System.out.println("Sent Msg: " + connectBS);
			System.out.println("To: " + sendPacket.getSocketAddress().toString());

			// Receive registration response from BootStrap Server
			DatagramPacket recv_pack = new DatagramPacket(recvData, recvData.length);
			sock.receive(recv_pack);
			String dataReceive = new String(recv_pack.getData(), 0, recv_pack.getLength());

			System.out.println();
			System.out.println("Rcvd Msg: " + dataReceive);
			System.out.println("From: " + recv_pack.getSocketAddress().toString());

			// Decoding the registration response
			StringTokenizer token = new StringTokenizer(dataReceive, " ");
			String Length = token.nextToken();
			String REGOK = token.nextToken();
			String no_nodes = token.nextToken();
			String IP_1, IP_2, Port_1, Port_2;

			if (no_nodes.equalsIgnoreCase("0")) {
				// System.out.println("Request is successfull. You are the first node in the network");
			} else if (no_nodes.equalsIgnoreCase("9999")) {
				System.out.println("Failed, Some Error in Command");
			} else if (no_nodes.equalsIgnoreCase("9998")) {
				System.out.println("Failed, Already registered to you, first unregister");
			} else if (no_nodes.equalsIgnoreCase("9997")) {
				System.out.println("Failed, registered to another user, try a different IP and Port");
			} else if (no_nodes.equalsIgnoreCase("9996")) {
				System.out.println("Failed, can't register, BS full");
			} else {
				// Only one node is already present in the network
				IP_1 = token.nextToken();
				Port_1 = token.nextToken();
				Node node1 = new Node(IP_1, Integer.parseInt(Port_1));
				nodeServ.addNeighbour(nodeServ, node1, false);

				// 2 or more nodes are already present in the network
				if (!no_nodes.equalsIgnoreCase("1")) {
					IP_2 = token.nextToken();
					Port_2 = token.nextToken();
					Node node2 = new Node(IP_2, Integer.parseInt(Port_2));
					// System.out.println("I shall connect to " + IP_2 + ":" +
					// Port_2);
					nodeServ.addNeighbour(nodeServ, node2, false);
				}
			}

		} catch (Exception e) {
			System.out.println("Error in connecting to bootstrap");
		}
	}

	// Unregistering from Bootstrap Server
	public void unregister_from_bootstrap(NodeServer nodeServ) throws IOException {
		byte[] sendData;
		byte[] recvData = new byte[5000];
		try {

			// IP and Port of Bootstrap server
			InetAddress BS_IP = InetAddress.getByName("planetlab-1.cs.colostate.edu");
			// InetAddress BS_IP =
			// InetAddress.getByName("plink.cs.uwaterloo.ca");
			int BS_port = 10000;

			// UnRegistration conenction string
			String connect_to_bs_1 = " UNREG " + InetAddress.getLocalHost().getHostAddress() + " " + sock.getLocalPort() + " div_han";
			String connectBS = AuxFunctions.getMsg(connect_to_bs_1);

			sendData = connectBS.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, BS_IP, BS_port);
			sock.send(sendPacket);

			System.out.println();
			System.out.println("Sent Msg: " + connectBS);
			System.out.println("To: " + sendPacket.getSocketAddress().toString());

		} catch (Exception e) {
			System.out.println("Error in connecting to bootstrap");
		}
	}

	// Receive UnReg response
	public void unregOKmsgReceive(NodeServer nodeServ, String msg) {
		StringTokenizer token = new StringTokenizer(msg, " ");
		String Length = token.nextToken();
		String UNREGOK = token.nextToken();
		String value = token.nextToken().trim();

		if (value.equalsIgnoreCase("0")) {
			System.out.println("Unregistered successfully.");
			nodeServ.unregOK = true;
		} else {
			System.out.println("Unregister not succesfull.");
		}

	}

	// Add neighbour nodes to current node
	public void addNeighbour(NodeServer nodeServ, Node Node1, Boolean received) {
		nodeServ.neighbourNodes.add(Node1);

		// If Join request is to be sent based on the registration response from
		// bootstrap giving one or two nodes to be added as neighbours
		if (!received) {
			// Send Join Request to neighbouring nodes.
			String join_message_1 = " JOIN " + nodeServ.curNode.nodeIP + " " + nodeServ.curNode.nodePort;
			String joinMsg = AuxFunctions.getMsg(join_message_1);

			try {
				byte[] sendData;
				sendData = joinMsg.getBytes();
				DatagramPacket sendPacket;
				sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(Node1.nodeIP), Node1.nodePort);
				sock.send(sendPacket);

				System.out.println();
				System.out.println("Sent Msg: " + joinMsg);
				System.out.println("To: " + sendPacket.getSocketAddress().toString());

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		// If join request is received
		else {
			Integer firstSN = 0; // =1 if this is the first SN

			// If total number of neighbours equals the required count for
			// becoming SN, then make this node a SN
			if (nodeServ.neighbourNodes.size() == nodeServ.minNodesForSN) {
				// Request SN list from previous SN, if exists
				if (nodeServ.curNode.hopsSN == 0) {
					// This is the first SN in the network
					// Since this is the first SN, it should reach every node.
					firstSN = 1;
				} else {
					// Add the previous SN to list of SuperNodes
					Node prevSN = new Node(nodeServ.curNode.superNodeIP, nodeServ.curNode.superNodePort);
					nodeServ.superNode = new SuperNode();

					// Do not add self into SN list
					if (!AuxFunctions.checkIfSelfNode(nodeServ.curNode.nodeIP, nodeServ.curNode.nodePort, prevSN.nodeIP, prevSN.nodePort)) {
						nodeServ.superNode.superNodesList.add(prevSN);
					}

					// Request SN list from the previous parent SN
					String reqSNlist = " REQ-SN-LIST " + nodeServ.curNode.nodeIP + " " + nodeServ.curNode.nodePort;
					String reqSNlistMsg = AuxFunctions.getMsg(reqSNlist);

					try {
						byte[] sendData;
						sendData = reqSNlistMsg.getBytes();
						DatagramPacket sendPacket;
						sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(nodeServ.curNode.superNodeIP),
								nodeServ.curNode.superNodePort);
						sock.send(sendPacket);

						System.out.println();
						System.out.println("Sent Msg: " + reqSNlistMsg);
						System.out.println("To: " + sendPacket.getSocketAddress().toString());

					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				// Set SN parameters
				nodeServ.isSN = true;
				nodeServ.curNode.superNodeIP = curNode.nodeIP;
				nodeServ.curNode.superNodePort = curNode.nodePort;
				nodeServ.curNode.hopsSN = 0;
				nodeServ.superNode = new SuperNode();
				nodeServ.superNode.IP = curNode.nodeIP;
				nodeServ.superNode.Port = curNode.nodePort;

				// Inform neighbours that this node has become a SN
				for (int i = 0; i < nodeServ.neighbourNodes.size(); i++) {
					nodeServ.sendSuperNodeInfo(nodeServ, nodeServ.neighbourNodes.get(i).nodeIP, nodeServ.neighbourNodes.get(i).nodePort, 0, firstSN);
				}

			}
			// If this is not becoming a SN with this connection, just send a
			// Join Ok message to the node that requeseted connection
			else {
				String joinOk_message_1 = " JOINOK " + curNode.superNodeIP + " " + curNode.superNodePort + " " + curNode.hopsSN;
				String joinOKMsg = AuxFunctions.getMsg(joinOk_message_1);

				try {
					byte[] sendData;
					sendData = joinOKMsg.getBytes();
					DatagramPacket sendPacket;
					sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(Node1.nodeIP), Node1.nodePort);
					sock.send(sendPacket);

					System.out.println();
					System.out.println("Sent Msg: " + joinOKMsg);
					System.out.println("To: " + sendPacket.getSocketAddress().toString());

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	// Receive SuperNode info
	public void receiveSuperNodeInfo(NodeServer nodeServ, String IP, Integer Port, Integer hops, Integer firstSN) {

		// General case
		if (nodeServ.isSN && firstSN == 0) {

			if (!AuxFunctions.checkIfSelfNode(nodeServ.curNode.nodeIP, nodeServ.curNode.nodePort, IP, Port)) {
				nodeServ.superNode.superNodesList.add(new Node(IP, Port));
			}
			nodeServ.superNode.superNodesList = AuxFunctions.removeDuplicateNode(nodeServ.superNode.superNodesList, IP, Port);
		}
		// If the current node is a SN and is receiving a SN info which claims
		// to be the first SN in the network.
		// This might happen when two nodes simultaneously become the first SNs.
		else if (nodeServ.isSN && firstSN == 1) {
			// Send the SN info to the other first SN and add it to its list of
			// SuperNodes also
			if (!AuxFunctions.checkIfSelfNode(nodeServ.curNode.nodeIP, nodeServ.curNode.nodePort, IP, Port)) {
				nodeServ.superNode.superNodesList.add(new Node(IP, Port));
			}
			nodeServ.superNode.superNodesList = AuxFunctions.removeDuplicateNode(nodeServ.superNode.superNodesList, IP, Port);

			String inform = " INFORM-SN " + nodeServ.curNode.nodeIP + " " + nodeServ.curNode.nodePort.toString();
			String informMsg = AuxFunctions.getMsg(inform);

			try {
				byte[] sendData;
				sendData = informMsg.getBytes();
				DatagramPacket sendPacket;
				sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(IP), Port);
				sock.send(sendPacket);

				System.out.println();
				System.out.println("Sent Msg: " + informMsg);
				System.out.println("To: " + sendPacket.getSocketAddress().toString());

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		// If current node is not a SN
		else {
			// check if its previous SN is at a larger distance than the new SN
			if ((nodeServ.curNode.hopsSN == 0) || ((hops + 1) < nodeServ.curNode.hopsSN)) {
				// If so, change the parent SN of this node.

				nodeServ.curNode.superNodeIP = IP;
				nodeServ.curNode.superNodePort = Port;
				nodeServ.curNode.hopsSN = hops + 1;

				// Send file list to SuperNode
				String joinChildMsg = " JOIN-AS-CHILD " + curNode.nodeIP + " " + curNode.nodePort.toString() + " " + curNode.hopsSN.toString() + " "
						+ curNode.num_files.toString();
				for (int i = 0; i < curNode.filenames.size(); i++) {
					joinChildMsg = joinChildMsg + " " + curNode.filenames.get(i);
				}
				String joinAsChild = AuxFunctions.getMsg(joinChildMsg);

				// Send message to SN
				try {
					byte[] sendData;
					sendData = joinAsChild.getBytes();
					DatagramPacket sendPacket;
					sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(IP), Port);
					sock.send(sendPacket);

					System.out.println();
					System.out.println("Sent Msg: " + joinAsChild);
					System.out.println("To: " + sendPacket.getSocketAddress().toString());

				} catch (IOException e) {
					e.printStackTrace();
				}

				// Send SN info to all neighbours
				for (int i = 0; i < nodeServ.neighbourNodes.size(); i++) {
					nodeServ.sendSuperNodeInfo(nodeServ, nodeServ.neighbourNodes.get(i).nodeIP, nodeServ.neighbourNodes.get(i).nodePort, hops + 1, 0);
				}
			}
			// If it receives the first SN, then forward to all neighbours no
			// matter if this node changes its parent SN or not
			else if (firstSN == 1) {
				for (int i = 0; i < nodeServ.neighbourNodes.size(); i++) {
					nodeServ.sendSuperNodeInfo(nodeServ, nodeServ.neighbourNodes.get(i).nodeIP, nodeServ.neighbourNodes.get(i).nodePort, hops + 1, 1);
				}
			}
		}
	}

	// Update parent SN info, if required
	public void setParentSuperNode(NodeServer nodeServ, Node node1, Integer hops) {

		if ((nodeServ.curNode.hopsSN == 0) || ((hops + 1) < nodeServ.curNode.hopsSN)) {

			nodeServ.curNode.superNodeIP = node1.nodeIP;
			nodeServ.curNode.superNodePort = node1.nodePort;
			nodeServ.curNode.hopsSN = hops + 1;

			String joinChildMsg = " JOIN-AS-CHILD " + curNode.nodeIP + " " + curNode.nodePort.toString() + " " + curNode.hopsSN.toString() + " "
					+ curNode.num_files.toString();
			for (int i = 0; i < curNode.filenames.size(); i++) {
				joinChildMsg = joinChildMsg + " " + curNode.filenames.get(i);
			}
			String joinAsChild = AuxFunctions.getMsg(joinChildMsg);

			// Send message to SN
			try {
				byte[] sendData;
				sendData = joinAsChild.getBytes();
				DatagramPacket sendPacket;
				sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(node1.nodeIP), node1.nodePort);
				sock.send(sendPacket);

				System.out.println();
				System.out.println("Sent Msg: " + joinAsChild);
				System.out.println("To: " + sendPacket.getSocketAddress().toString());

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// If node becomes SN, then broadcast this info
	public void sendSuperNodeInfo(NodeServer nodeServ, String dstNodeIP, Integer dstNodePort, Integer hops, Integer firstSN) {

		// Message: Length BROADCAST-SN IP_address Port_No Hop_Distance
		String broadcast = " BROADCAST-SN " + nodeServ.curNode.superNodeIP + " " + nodeServ.curNode.superNodePort.toString() + " "
				+ nodeServ.curNode.hopsSN.toString() + " " + firstSN;
		String broadcastmsg = AuxFunctions.getMsg(broadcast);

		try {
			byte[] sendData;
			sendData = broadcastmsg.getBytes();
			DatagramPacket sendPacket;
			sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(dstNodeIP), dstNodePort);
			sock.send(sendPacket);

			System.out.println();
			System.out.println("Sent Msg: " + broadcastmsg);
			System.out.println("To: " + sendPacket.getSocketAddress().toString());

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// On receiving JOIN-AS-CHILD request by a SN, add the node as a child
	public void setChildNodes(NodeServer nodeServ, String rcvdMsg) throws NumberFormatException, UnknownHostException {

		String msg[] = rcvdMsg.split(" ");
		Node childNode = new Node(msg[2].trim(), Integer.parseInt(msg[3].trim()));

		childNode.num_files = Integer.parseInt(msg[5].trim());
		childNode.filenames = new ArrayList<String>();

		for (int i = 1; i <= childNode.num_files; i++) {
			childNode.filenames.add(msg[5 + i].trim());
		}

		ChildNodeInfo child = new ChildNodeInfo(childNode, Integer.parseInt(msg[4].trim()));
		nodeServ.superNode.childNodes.add(child);

	}

	// Print neighbours of current node
	public void printNeighbours(NodeServer nodeServ) {
		System.out.println();
		try {
			System.out
					.println(InetAddress.getByName(nodeServ.curNode.nodeIP).getHostName() + ":" + nodeServ.curNode.nodePort + " 's neighbours are:");
			for (int i = 0; i < nodeServ.neighbourNodes.size(); i++) {
				System.out.println(nodeServ.neighbourNodes.get(i).nodeIP + ":" + nodeServ.neighbourNodes.get(i).nodePort);
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	// Print parent SN and also list of SNs incase this node is a SN
	public void printSuperNode(NodeServer nodeServ) {
		System.out.println();
		System.out.println("Parent SuperNode= " + nodeServ.curNode.superNodeIP + ":" + nodeServ.curNode.superNodePort);

		if (nodeServ.isSN) {
			System.out.println("SuperNodesList:");
			for (int i = 0; i < nodeServ.superNode.superNodesList.size(); i++) {
				System.out.println(nodeServ.superNode.superNodesList.get(i).nodeIP + ":" + nodeServ.superNode.superNodesList.get(i).nodePort);
			}
		}
	}

	// Print files present in this peer
	public void printFilenames(NodeServer nodeServ) {
		System.out.println();
		System.out.println("Existing files: ");
		for (int i = 0; i < nodeServ.curNode.filenames.size(); i++) {
			System.out.println(nodeServ.curNode.filenames.get(i));
		}
	}

	// Print childnodes in case this is a SN
	public void printChildNodes(NodeServer nodeServ) {
		System.out.println();
		if (!nodeServ.isSN)
			System.out.println("Not a SuperNode.");
		else {
			System.out.println("Child Nodes: ");
			for (int i = 0; i < nodeServ.superNode.childNodes.size(); i++) {
				System.out.print(nodeServ.superNode.childNodes.get(i).childNode.nodeIP + ":"
						+ nodeServ.superNode.childNodes.get(i).childNode.nodePort);
				for (int j = 0; j < nodeServ.superNode.childNodes.get(i).childNode.filenames.size(); j++) {
					System.out.print(" " + nodeServ.superNode.childNodes.get(i).childNode.filenames.get(j));
				}
				System.out.println();
			}
		}
	}

	// Print # query messages
	public void printMessages(NodeServer nodeServ) {
		System.out.println("# Queries Received: " + nodeServ.rcvdMsgs);
		System.out.println("# Queries Forwarded: " + nodeServ.frwdMsgs);
		System.out.println("# Queries Answered: " + nodeServ.answdMsgs);
	}

	// Add a new SuperNode to list of SNs
	public void addNewSuperNode(NodeServer nodeServ, String msg, Integer sendList) {

		String msgs[] = msg.split(" ");

		Node node = new Node(msgs[2], Integer.parseInt(msgs[3].trim()));

		// Add new SN to list
		if (!AuxFunctions.checkIfSelfNode(nodeServ.curNode.nodeIP, nodeServ.curNode.nodePort, node.nodeIP, node.nodePort)) {
			nodeServ.superNode.superNodesList.add(node);
		}
		// Remove duplicates from the list
		nodeServ.superNode.superNodesList = AuxFunctions.removeDuplicateNode(nodeServ.superNode.superNodesList, node.nodeIP, node.nodePort);

		if (sendList == 1) {
			// sendList = 1 implies that the new SN is requesting SN list from
			// this node
			// Send list of all SuperNodes to requesting SN
			nodeServ.sync = false; // to check if the nodes are synced

			nodeServ.lastSendSNList = nodeServ.superNode.superNodesList.size();
			nodeServ.lastsentSNListNode = new Node(node.nodeIP, node.nodePort);

			String sendSNListMsg;

			sendSNListMsg = " SN-LIST " + new Integer((nodeServ.superNode.superNodesList.size() + 1)).toString() + " " + nodeServ.curNode.nodeIP
					+ " " + nodeServ.curNode.nodePort;

			for (int i = 0; i < nodeServ.superNode.superNodesList.size(); i++) {
				sendSNListMsg = sendSNListMsg + " " + nodeServ.superNode.superNodesList.get(i).nodeIP + " "
						+ nodeServ.superNode.superNodesList.get(i).nodePort;
			}
			String SNListmsg = AuxFunctions.getMsg(sendSNListMsg);

			try {
				byte[] sendData;
				sendData = SNListmsg.getBytes();
				DatagramPacket sendPacket;
				sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(node.nodeIP), node.nodePort);
				sock.send(sendPacket);

				System.out.println();
				System.out.println("Sent Msg: " + SNListmsg);
				System.out.println("To: " + sendPacket.getSocketAddress().toString());

			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			// Just add the new SN to list of SNs and if not synced, send the
			// new list to the last SN that requested list
			if (nodeServ.lastSendSNList != nodeServ.superNode.superNodesList.size() && !nodeServ.sync) {
				nodeServ.sync = true;
				// Send the SN list to the last node that received SN list from
				// this node to synchronize.
				String sendSNListMsg;

				sendSNListMsg = " SN-LIST " + new Integer((nodeServ.superNode.superNodesList.size() + 1)).toString() + " " + nodeServ.curNode.nodeIP
						+ " " + nodeServ.curNode.nodePort;

				for (int i = 0; i < nodeServ.superNode.superNodesList.size(); i++) {
					sendSNListMsg = sendSNListMsg + " " + nodeServ.superNode.superNodesList.get(i).nodeIP + " "
							+ nodeServ.superNode.superNodesList.get(i).nodePort;
				}
				String SNListmsg = AuxFunctions.getMsg(sendSNListMsg);

				try {
					byte[] sendData;
					sendData = SNListmsg.getBytes();
					DatagramPacket sendPacket;
					sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(node.nodeIP), node.nodePort);
					sock.send(sendPacket);

					System.out.println();
					System.out.println("Sent Msg: " + SNListmsg);
					System.out.println("To: " + sendPacket.getSocketAddress().toString());

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// Inform other SNs about its new status of SN
	public void informSuperNodes(String informMsg, String IP, Integer Port) {

		try {
			byte[] sendData;
			sendData = informMsg.getBytes();
			DatagramPacket sendPacket;
			sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(IP), Port);
			sock.send(sendPacket);

			System.out.println();
			System.out.println("Sent Msg: " + informMsg);
			System.out.println("To: " + sendPacket.getSocketAddress().toString());

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	// Inform SNs received from SN-List message about new status of SN
	public void receiveSuperNodeList(NodeServer nodeServ, String msg) {
		String list[] = msg.split(" ");

		String inform = " INFORM-SN " + nodeServ.curNode.nodeIP + " " + nodeServ.curNode.nodePort.toString();
		String informMsg = AuxFunctions.getMsg(inform);
		System.out.println(informMsg);

		for (int i = 0; i < Integer.parseInt(list[2].trim()); i++) {
			Node node = new Node(list[3 + 2 * i], new Integer(list[3 + 2 * i + 1].trim()));
			if (!AuxFunctions.checkIfSelfNode(nodeServ.curNode.nodeIP, nodeServ.curNode.nodePort, node.nodeIP, node.nodePort)) {
				nodeServ.superNode.superNodesList.add(node);
			}
			nodeServ.superNode.superNodesList = AuxFunctions.removeDuplicateNode(nodeServ.superNode.superNodesList, node.nodeIP, node.nodePort);
			informSuperNodes(informMsg, list[3 + 2 * i], new Integer(list[3 + 2 * i + 1].trim()));
		}
	}

	//Query search
	public void searchQuery(NodeServer nodeServ, String searchQuery) {

		nodeServ.rcvdMsgs++;

		// Start Timer
		nodeServ.queryStartTime = String.format("%.3f", System.currentTimeMillis() / 1000.0);

		// Search in self files
		Boolean found = false;
		String filePrsnt = null;
		filePrsnt = SearchFiles.search(nodeServ.curNode.filenames, searchQuery);

		//If file found in self node
		if (filePrsnt != "") {
			nodeServ.queryEndTime = String.format("%.3f", System.currentTimeMillis() / 1000.0);
			double delay_1 = Double.parseDouble(nodeServ.queryEndTime);
			double delay_2 = Double.parseDouble(nodeServ.queryStartTime);
			double delay = delay_1 - delay_2;

			System.out.println("File: " + filePrsnt + ", present in Current Node: " + nodeServ.curNode.nodeIP + ":" + nodeServ.curNode.nodePort);
			System.out.println("Time: " + delay);

			nodeServ.answdMsgs++;

		} else {
			// check if the current node is a SuperNode. If so, check in its
			// child nodes
			if (nodeServ.isSN) {

				for (int i = 0; i < nodeServ.superNode.childNodes.size(); i++) {
					filePrsnt = SearchFiles.search(nodeServ.superNode.childNodes.get(i).childNode.filenames, searchQuery);

					//If file found in a child node
					if (filePrsnt != "") {

						nodeServ.queryEndTime = String.format("%.3f", System.currentTimeMillis() / 1000.0);
						double delay_1 = Double.parseDouble(nodeServ.queryEndTime);
						double delay_2 = Double.parseDouble(nodeServ.queryStartTime);
						double delay = delay_1 - delay_2;

						System.out.println("File: " + filePrsnt + ", present in Child Node: " + nodeServ.superNode.childNodes.get(i).childNode.nodeIP
								+ ":" + nodeServ.curNode.nodePort);
						System.out.println("Time: " + delay);

						found = true;
						nodeServ.answdMsgs++;
						// System.out.println("File fould in child node");
						break;
					}
				}
				//If file not found in child nodes also, then send search request to all SNs
				if (!found) {
					// Send Search Request to all SuperNodes
					for (int i = 0; i < nodeServ.superNode.superNodesList.size(); i++) {
						nodeServ.searchReqToSN(nodeServ, nodeServ.curNode.nodeIP, nodeServ.curNode.nodePort, searchQuery, 1, 1,
								nodeServ.superNode.superNodesList.get(i).nodeIP, nodeServ.superNode.superNodesList.get(i).nodePort);
					}
					nodeServ.frwdMsgs++;
				}

			} 
			//This node is not a SN. Send Search request to parent SN
			else {
				// Send request to parent SuperNode to search the file
				nodeServ.searchReqToSN(nodeServ, nodeServ.curNode.nodeIP, nodeServ.curNode.nodePort, searchQuery, 0, 1, nodeServ.curNode.superNodeIP,
						nodeServ.curNode.superNodePort);
				nodeServ.frwdMsgs++;
			}
		}
	}

	//Search request to SN
	public void searchReqToSN(NodeServer nodeServ, String reqNodeIP, Integer reqNodePort, String searchQuery, Integer fromSNOrNot, Integer hopCount,
			String SN_IP, Integer SN_Port) {
		/*
		 * Message Format: Length SEARCH_SN Requesting_Node_IP
		 * Requesting_Node_Port From_Child_Node_Or_SN Hop_Count Search_Query
		 */
		String search_in_SN = (" SEARCH_SN " + reqNodeIP + " " + reqNodePort + " " + fromSNOrNot + " " + hopCount + " " + searchQuery);
		String searchReq = AuxFunctions.getMsg(search_in_SN);

		try {
			byte[] sendData = searchReq.getBytes();
			DatagramPacket sendPacket;

			sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(SN_IP), SN_Port);
			sock.send(sendPacket);

			System.out.println();
			System.out.println("Sent Msg: " + searchReq);
			System.out.println("To: " + sendPacket.getSocketAddress().toString());

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//Process Search request received from a node
	public void processSearchRequest(NodeServer nodeServ, String searchReq) {

		Boolean found = false;
		String filePrsnt = null;

		String searchQ = "";
		String msg[] = searchReq.split(" ");
		Integer hops = Integer.parseInt(msg[5]);
		for (int t = 6; t < msg.length; t++) {
			searchQ = searchQ + " " + msg[t];
		}
		searchQ = searchQ.trim();

		// Check in current SuperNode
		filePrsnt = SearchFiles.search(nodeServ.curNode.filenames, searchQ);
		
		//If file found in current SN
		if (filePrsnt != "") {		
			// Send search result to requesting node
			nodeServ.sendSearchResult(nodeServ, msg[2], Integer.parseInt(msg[3]), hops, filePrsnt, nodeServ.curNode.nodeIP, nodeServ.curNode.nodePort);
			found = true;
			nodeServ.answdMsgs++;
		} else // check in its Child Nodes
		{
			for (int i = 0; i < nodeServ.superNode.childNodes.size(); i++) {
				filePrsnt = SearchFiles.search(nodeServ.superNode.childNodes.get(i).childNode.filenames, searchQ);
				//If file is found in a child node
				if (filePrsnt != "") {					
					found = true;
					// Send results back to requesting node
					nodeServ.sendSearchResult(nodeServ, msg[2], Integer.parseInt(msg[3]), hops + 1, filePrsnt,
							nodeServ.superNode.childNodes.get(i).childNode.nodeIP, nodeServ.superNode.childNodes.get(i).childNode.nodePort);
					nodeServ.answdMsgs++;
					break;
				}
			}
		}
		if (!found) {
			// if the request is coming from a childNode, pass it on to other
			// SNs with increased hop count
			if (msg[4].equalsIgnoreCase("0")) {
				for (int i = 0; i < nodeServ.superNode.superNodesList.size(); i++) {
					nodeServ.searchReqToSN(nodeServ, msg[2], Integer.parseInt(msg[3]), searchQ, 1, 2,
							nodeServ.superNode.superNodesList.get(i).nodeIP, nodeServ.superNode.superNodesList.get(i).nodePort);
				}
			}
			nodeServ.frwdMsgs++;
		}
	}

	//Send search found result to requesting node
	public void sendSearchResult(NodeServer nodeServ, String reqNodeIP, Integer reqNodePort, Integer reqNodeHops, String filename,
			String foundAtNode_IP, Integer founfAtNode_Port) {
		// Message Format: Length FOUND atNode_IP, atNode_Port, atNode_hops,
		// filename
		String foundStr = (" FOUND " + foundAtNode_IP + " " + founfAtNode_Port + " " + reqNodeHops + " " + filename);
		String foundMsg = AuxFunctions.getMsg(foundStr);
		// System.out.println("Response: " + foundMsg +
		// " sent to requesting Node at IP Address " + reqNodeIP + " at Port " +
		// reqNodePort);
		try {
			byte[] sendData = foundMsg.getBytes();
			DatagramPacket sendPacket;

			sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(reqNodeIP), reqNodePort);
			sock.send(sendPacket);

			System.out.println();
			System.out.println("Sent Msg: " + foundMsg);
			System.out.println("To: " + sendPacket.getSocketAddress().toString());

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//Display file found results
	public void foundFile(String foundResponse, String quersyStartTime, String queryEndTime) {
		String resp[] = foundResponse.split(" ");
		System.out.println("File:" + resp[5] + " found in node: " + resp[2] + ":" + resp[3] + " at a hop count of " + resp[4]);

		double delay_1 = Double.parseDouble(queryEndTime);
		double delay_2 = Double.parseDouble(quersyStartTime);
		double delay = delay_1 - delay_2;

		System.out.println("Time: " + delay);
	}

	//Node leaves the Distributed System
	public void leaveDS(NodeServer nodeServ) throws IOException {
		// Unregister from BootStrap Server
		nodeServ.unregister_from_bootstrap(nodeServ);

		// Send LEAVE message to neighbours and SN
		for (int i = 0; i < nodeServ.neighbourNodes.size(); i++) {
			nodeServ.sendLeaveMsg(nodeServ, nodeServ.neighbourNodes.get(i).nodeIP, nodeServ.neighbourNodes.get(i).nodePort, 0);
		}

		if (nodeServ.isSN) {
			// Send Leave Message to all other SNs
			for (int i = 0; i < nodeServ.superNode.superNodesList.size(); i++) {
				nodeServ.sendLeaveMsg(nodeServ, nodeServ.superNode.superNodesList.get(i).nodeIP, nodeServ.superNode.superNodesList.get(i).nodePort, 2);
			}

			// Send Leave msg to all child nodes
			for (int j = 0; j < nodeServ.superNode.childNodes.size(); j++) {
				nodeServ.sendLeaveMsg(nodeServ, nodeServ.superNode.childNodes.get(j).childNode.nodeIP,
						nodeServ.superNode.childNodes.get(j).childNode.nodePort, 3);
			}

		} else {
			// Send LEAVE message to neighbours and SN
			for (int i = 0; i < nodeServ.neighbourNodes.size(); i++) {
				nodeServ.sendLeaveMsg(nodeServ, nodeServ.neighbourNodes.get(i).nodeIP, nodeServ.neighbourNodes.get(i).nodePort, 0);
			}

			nodeServ.sendLeaveMsg(nodeServ, nodeServ.curNode.superNodeIP, nodeServ.curNode.superNodePort, 1);

		}

		//Wait to receive the unReg message from BootStrap server
		while (!nodeServ.unregOK) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			// System.out.println("sock close");
			// nodeServ.sock.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//Send leave Message to peers
	public void sendLeaveMsg(NodeServer nodeServ, String IP, Integer Port, Integer fromNeighbourOrSN) throws IOException {
		String leaveMessage = " LEAVE " + nodeServ.curNode.nodeIP + " " + nodeServ.curNode.nodePort + " " + fromNeighbourOrSN;
		String leaveMsg = AuxFunctions.getMsg(leaveMessage);

		try {
			byte[] sendData = leaveMsg.getBytes();
			DatagramPacket sendPacket;

			sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(IP), Port);
			// System.out.println("Leave Request: " + leaveMsg + " sent to : " +
			// IP + ":" + Port);
			sock.send(sendPacket);

			System.out.println();
			System.out.println("Sent Msg: " + leaveMsg);
			System.out.println("To: " + sendPacket.getSocketAddress().toString());

		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

	}

	// Receive a Leave message from a peer
	public void receiveLeaveMsg(NodeServer nodeServ, String IP, Integer Port, Integer flag) {
		if (flag == 0) {
			// Received from neighbour
			// Remove the node from neighbours list
			nodeServ.neighbourNodes = AuxFunctions.removeNode(nodeServ.neighbourNodes, IP, Port);

		} else if (flag == 1) {
			nodeServ.superNode.childNodes = AuxFunctions.removeChildNode(nodeServ.superNode.childNodes, IP, Port);
		} else if (flag == 2) {
			// Remove childnode from SN
			nodeServ.superNode.superNodesList = AuxFunctions.removeNode(nodeServ.superNode.superNodesList, IP, Port);
		} else {
			// Remove SN from this child node
			nodeServ.curNode.superNodeIP = "";
			nodeServ.curNode.superNodePort = 0;
			nodeServ.curNode.hopsSN = 0;

			String snInfo = " SN-INFO " + nodeServ.curNode.nodeIP + " " + nodeServ.curNode.nodePort;
			String SNinfoMsg = AuxFunctions.getMsg(snInfo);

			for (int k = 0; k < nodeServ.neighbourNodes.size(); k++) {
				try {
					byte[] sendData = SNinfoMsg.getBytes();
					DatagramPacket sendPacket;

					sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(nodeServ.neighbourNodes.get(k).nodeIP),
							nodeServ.neighbourNodes.get(k).nodePort);
					sock.send(sendPacket);

					System.out.println();
					System.out.println("Sent Msg: " + SNinfoMsg);
					System.out.println("To: " + sendPacket.getSocketAddress().toString());

				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	//Send current node SN info
	public void sendSNInfo(NodeServer nodeServ, String IP, Integer Port) {
		String send = " MY-SN " + nodeServ.curNode.superNodeIP + " " + nodeServ.curNode.superNodePort + " " + nodeServ.curNode.hopsSN;
		String sendMsg = AuxFunctions.getMsg(send);

		try {
			byte[] sendData = sendMsg.getBytes();
			DatagramPacket sendPacket;

			sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(IP), Port);
			sock.send(sendPacket);

			System.out.println();
			System.out.println("Sent Msg: " + sendMsg);
			System.out.println("To: " + sendPacket.getSocketAddress().toString());

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//Receive new SN info. (required when parent SN leaves the network)
	public void receiveSNInfo(NodeServer nodeServ, String IP, Integer Port, Integer hops) {
		if (Port != 0 && IP != "") {
			if (nodeServ.curNode.hopsSN == 0 || nodeServ.curNode.hopsSN > (hops + 1)) {
				nodeServ.curNode.superNodeIP = IP;
				nodeServ.curNode.superNodePort = Port;
				nodeServ.curNode.hopsSN = hops + 1;

				// Request SN to add it as Child
				String joinChildMsg = " JOIN-AS-CHILD " + curNode.nodeIP + " " + curNode.nodePort.toString() + " " + curNode.hopsSN.toString() + " "
						+ curNode.num_files.toString();
				for (int i = 0; i < curNode.filenames.size(); i++) {
					joinChildMsg = joinChildMsg + " " + curNode.filenames.get(i);
				}
				String joinAsChild = AuxFunctions.getMsg(joinChildMsg);

				// Send message to SN
				try {
					byte[] sendData;
					sendData = joinAsChild.getBytes();
					DatagramPacket sendPacket;
					sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(IP), Port);
					sock.send(sendPacket);

					System.out.println();
					System.out.println("Sent Msg: " + joinAsChild);
					System.out.println("To: " + sendPacket.getSocketAddress().toString());

				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}
	}

	public static void main(String[] args) throws IOException {

		// creating main Node class. This is the class that will be called to
		// create each Node.
		NodeServer NodeServ = new NodeServer();
		NodeServ.connect_to_bootstrap(NodeServ);

		if (args.length > 0) {
			try {
				NodeServ.minNodesForSN = Integer.parseInt(args[0]);
			} catch (NumberFormatException ex) {
				System.out.println("The argument passed is not of the correct Data-type.");
			}
		} else {
			NodeServ.minNodesForSN = 4;
		}

		// Open a Console Listener for Commands
		new Thread(new ConsoleReader(NodeServ)).start();

		// Listen for messages
		DatagramSocket serverSocket = sock;

		while (true) {
			byte[] receiveData = new byte[5000];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			serverSocket.receive(receivePacket);
			String rcvdPacket = new String(receivePacket.getData());

			// System.out.println("Request: " + rcvdPacket);
			System.out.println();
			System.out.println("Rcvd Msg: " + rcvdPacket);
			System.out.println("From: " + receivePacket.getSocketAddress().toString());

			String msgType = rcvdPacket.split(" ")[1].trim();
			switch (msgType) {
			case "JOIN":
				Node node1 = new Node(rcvdPacket.split(" ")[2], Integer.parseInt(rcvdPacket.split(" ")[3].trim()));
				NodeServ.addNeighbour(NodeServ, node1, true);
				break;

			case "JOINOK":
				if (!rcvdPacket.split(" ")[2].trim().equalsIgnoreCase("null")) {
					Node node2 = new Node(rcvdPacket.split(" ")[2].trim(), Integer.parseInt(rcvdPacket.split(" ")[3].trim()));
					NodeServ.setParentSuperNode(NodeServ, node2, Integer.parseInt(rcvdPacket.split(" ")[4].trim()));
				}
				break;

			case "BROADCAST-SN":
				NodeServ.receiveSuperNodeInfo(NodeServ, rcvdPacket.split(" ")[2].trim(), Integer.parseInt(rcvdPacket.split(" ")[3].trim()),
						Integer.parseInt(rcvdPacket.split(" ")[4].trim()), Integer.parseInt(rcvdPacket.split(" ")[5].trim()));
				break;

			case "JOIN-AS-CHILD":
				NodeServ.setChildNodes(NodeServ, rcvdPacket);
				break;

			case "REQ-SN-LIST":
				NodeServ.addNewSuperNode(NodeServ, rcvdPacket, 1);
				break;

			case "SN-LIST":
				NodeServ.receiveSuperNodeList(NodeServ, rcvdPacket);
				break;

			case "INFORM-SN":
				NodeServ.addNewSuperNode(NodeServ, rcvdPacket, 0);
				break;

			case "SEARCH_SN":
				NodeServ.rcvdMsgs++;
				NodeServ.processSearchRequest(NodeServ, rcvdPacket);
				break;

			case "FOUND":
				NodeServ.queryEndTime = String.format("%.3f", System.currentTimeMillis() / 1000.0);
				NodeServ.foundFile(rcvdPacket, NodeServ.queryStartTime, NodeServ.queryEndTime);
				break;

			case "UNROK":
				NodeServ.unregOKmsgReceive(NodeServ, rcvdPacket);
				break;

			case "LEAVE":
				NodeServ.receiveLeaveMsg(NodeServ, rcvdPacket.split(" ")[2], Integer.parseInt(rcvdPacket.split(" ")[3].trim()),
						Integer.parseInt(rcvdPacket.split(" ")[4].trim()));
				break;

			case "SN-INFO":
				NodeServ.sendSNInfo(NodeServ, rcvdPacket.split(" ")[2], Integer.parseInt(rcvdPacket.split(" ")[3].trim()));
				break;

			case "MY-SN":
				NodeServ.receiveSNInfo(NodeServ, rcvdPacket.split(" ")[2], Integer.parseInt(rcvdPacket.split(" ")[3].trim()),
						Integer.parseInt(rcvdPacket.split(" ")[4].trim()));
				break;
			}

		}

	}

}
