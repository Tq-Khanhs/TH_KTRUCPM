package State_Pattern;

public class GiamDocState implements NhanVienState {

	@Override
	public void doTask(NhanVien nhanVien) {
		System.out.println("Ten: "+nhanVien.getName() +"   Chuc vu: "+ "Giam Doc");
		System.out.println("Giao viec cho nhan vien");
		
	}

}
