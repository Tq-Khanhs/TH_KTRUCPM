package State_Pattern;

public class NhanVien {
	private String name;
	NhanVienState state;
	
	
	public NhanVien(String name, NhanVienState state) {
		super();
		this.name = name;
		this.state = state;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}



	public NhanVienState getState() {
		return state;
	}


	public void setState(NhanVienState state) {
		this.state = state;
	}


	public void doTask() {
		state.doTask(this);
	}
	
	
}

