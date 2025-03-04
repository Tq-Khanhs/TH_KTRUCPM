package Strategy_Pattern;

public class NhanVienVanPhongStrategy implements NhanVienStrategy {

	@Override
	public void doTask() {
		System.out.println("Lam viec trong van phong");
	}

}
