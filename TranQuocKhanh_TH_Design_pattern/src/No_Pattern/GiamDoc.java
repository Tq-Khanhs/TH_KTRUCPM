package No_Pattern;

public class GiamDoc extends Employee {

	public GiamDoc(String name) {
		super(name, "Giam Doc");
		// TODO Auto-generated constructor stub
	}

	@Override
	public void doTask() {
		System.out.println("Ten: "+name +"   Chuc vu: "+ chucVu);
		System.out.println("Giao viec cho nhan vien");
		
	}



	

}
