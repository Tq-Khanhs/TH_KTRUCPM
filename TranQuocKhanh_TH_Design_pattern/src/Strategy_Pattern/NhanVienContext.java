package Strategy_Pattern;

public class NhanVienContext {
	private String name;
	private String chucVu;
	NhanVienStrategy strategy;
	public NhanVienContext(String name, String chucVu, NhanVienStrategy strategy) {
		super();
		this.name = name;
		this.chucVu = chucVu;
		this.strategy = strategy;
	}
	
	
	
	public void setStrategy(NhanVienStrategy strategy) {
		this.strategy= strategy;
	}
	
	public void doTask() {
		System.out.println("Ten: "+name +"Chuc vu: "+ chucVu);
		strategy.doTask();
	}
	
	
	

}
