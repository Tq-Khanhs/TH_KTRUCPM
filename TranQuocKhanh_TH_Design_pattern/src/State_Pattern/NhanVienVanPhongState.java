package State_Pattern;

public class NhanVienVanPhongState implements NhanVienState{

	@Override
	public void doTask(NhanVien nhanVien) {
		System.out.println("Ten: "+nhanVien.getName() +"   Chuc vu: "+ "Nhan Vien Van Phong");
		System.out.println("Lam viec trong van phong");
		
	}
	

}
