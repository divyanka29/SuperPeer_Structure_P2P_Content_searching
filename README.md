# SuperPeer_Structure_P2P_Content_searching
This assignment has been coded in Java and does not require any make file for execution.

To compile the code, go to the "src" folder in the Submission and compile using "javac":
	javac *.java
This will create the .class files

To start a node, go to same folder (which contains the .class files, in this case "src" folder) and type the following command in the terminal:
	java NodeServer <optional-minimum_number_of_neighbours_to_form_SN>

This will start the node at the local host at a random port and send registeration request ot bootstrap server at planetlab-1.cs.colostate.edu:10000

The node also contains a console reader. To search for a filename, type the search query in the console of the node that starts the search.
