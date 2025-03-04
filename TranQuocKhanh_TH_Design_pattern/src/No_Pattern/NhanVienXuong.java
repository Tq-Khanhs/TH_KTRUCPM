package No_Pattern;

public class NhanVienXuong extends Employee {

	public NhanVienXuong(String name) {
		super(name, "Nhan vien xuong");
		// TODO Auto-generated constructor stub
	}

	@Override
	public void doTask() {
		System.out.println("Ten: "+name +"Chuc vu: "+ chucVu);
		System.out.println("Lam viec trong xuong");
		
	}



	

}

