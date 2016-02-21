package AdhocCollaboration;

public class UserMain {
	public UserMain() {
	};

	public void start() {
		String[] args = new String[]{};
//		String[] args = new String[] {"/Users/BinChen/Documents/workspaceReapstProblem/AdhocCollaboration/AdhocCollaboration.rs"};
		repast.simphony.runtime.RepastMain.main(args);
	}

	public static void main(String[] args) {
		UserMain um = new UserMain();
		um.start();
	}
}