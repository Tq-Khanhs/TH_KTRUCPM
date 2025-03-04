package No_Pattern;

public class NhanVienVanPhong extends Employee {

	public NhanVienVanPhong(String name) {
		super(name, "Nhan vien van phong");
		// TODO Auto-generated constructor stub
	}

	@Override
	public void doTask() {
		System.out.println("Ten: "+name +"Chuc vu: "+ chucVu);
		System.out.println("Lam viec trong van phong");
		
	}



	

}
