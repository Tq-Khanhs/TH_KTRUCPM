package State_Pattern;

public class NhanVienXuongState implements NhanVienState {

	@Override
	public void doTask(NhanVien nhanVien) {
		System.out.println("Ten: "+nhanVien.getName() +"   Chuc vu: "+ "Nhan Vien Xuong");
		System.out.println("Lam viec trong xuong");
		
	}

}
