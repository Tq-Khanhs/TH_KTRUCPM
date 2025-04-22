package main;

import No_Pattern.BaoVe;
import No_Pattern.Employee;
import No_Pattern.GiamDoc;
import No_Pattern.NhanVienVanPhong;
import No_Pattern.NhanVienXuong;
import State_Pattern.BaoVeState;
import State_Pattern.GiamDocState;
import State_Pattern.NhanVien;
import State_Pattern.NhanVienVanPhongState;
import State_Pattern.NhanVienXuongState;

public class Demo {
	public static void main(String[] args) {
//		No pattern
//		Employee nhanVien1 = new GiamDoc("Anh A"); 
//		nhanVien1.doTask();
//		
//		Employee nhanVien2 = new NhanVienVanPhong("Anh B"); 
//		nhanVien2.doTask();
//		
//		Employee nhanVien3 = new BaoVe("Anh C"); 
//		nhanVien3.doTask();
//		
//		Employee nhanVien4 = new NhanVienXuong("Anh D"); 
//		nhanVien4.doTask();
		
//		State pattern
		
		NhanVien nhanVien = new NhanVien("NguyenVanA", new GiamDocState());
		nhanVien.doTask();
		
		nhanVien.setState(new BaoVeState());
		nhanVien.doTask();
		
		nhanVien.setState(new NhanVienXuongState());
		nhanVien.doTask();
		
		nhanVien.setState(new NhanVienVanPhongState());
		nhanVien.doTask();
	}

}
