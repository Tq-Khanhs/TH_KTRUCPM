package No_Pattern;

public class BaoVe extends Employee {

	public BaoVe(String name) {
		super(name, "Bao ve");
		// TODO Auto-generated constructor stub
	}

	@Override
	public void doTask() {
		System.out.println("Ten: "+name +"Chuc vu: "+ chucVu);
		System.out.println("Giu gin an ninh");
		
	}



	

}
