package State_Pattern;

public class BaoVeState implements NhanVienState{

	@Override
	public void doTask(NhanVien nhanVien) {
		System.out.println("Ten: "+nhanVien.getName() +"   Chuc vu: "+ "Bao Ve");
		System.out.println("Giu gin an ninh");
		
	}

}
