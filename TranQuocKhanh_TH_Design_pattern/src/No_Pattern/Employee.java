package No_Pattern;

public abstract class Employee {
	protected String name;
	protected String chucVu;
	public Employee(String name, String chucVu) {
		super();
		this.name = name;
		this.chucVu = chucVu;
	}
	
	

	public abstract void doTask();
	
	
	

}
