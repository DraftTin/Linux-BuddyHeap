import java.util.Arrays;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

class Config{
	private int initialized;
	private final int NUM = 10;
	private List<Integer> memSizeOpt;
	private List<Integer> pageSizeOpt;
	private List<Integer> alloSizeOpt;
	private int memSize;
	private int pageSize;
	private Scanner input = null;
	public Config() {
		input = new Scanner(System.in);
		initialized = 0;
		memSizeOpt = Arrays.asList(256, 512);	// MB
		pageSizeOpt = Arrays.asList(1, 2, 4, 256);	// KB
		alloSizeOpt = new ArrayList<Integer>();	// 能够申请的内存大小
		int alloSize = 1;
		for(int i = 0; i < NUM; ++i) {
			alloSizeOpt.add(alloSize);
			alloSize *= 2;
		}
		memSize = 0;
		pageSize = 0;
	}
	public int getNUM() {
		return NUM;
	}
	public List<Integer> getAlloSizeOpt() {
		return alloSizeOpt;
	}
	public void setMemSizeOpt(List<Integer> memSizeOpt) {
		this.memSizeOpt = memSizeOpt;
	}
	public List<Integer> getMemSizeOpt() {
		return memSizeOpt;
	}
	public void setPageSizeOpt(List<Integer> pageSizeOpt) {
		this.pageSizeOpt = pageSizeOpt;
	}
	public List<Integer> getPageSizeOpt() {
		return pageSizeOpt;
	}
	public void setMemSize(int size) {
		memSize = size;
	}
	public int getMemSize() {
		assert initialized == 1 : "not safe";
		return memSize;
	}
	public void setPageSize(int size) {
		pageSize = size;
	}
	public int getPageSize() {
		assert initialized == 1 : "not safe";
		return pageSize;
	}
	public void init() {
		System.out.print("Select memory size: ");
		for(int i = 1; i <= memSizeOpt.size(); ++i) {
			System.out.print(i + ". " + memSizeOpt.get(i - 1) + "MB  ");
		}
		System.out.println();
		int opt = input.nextInt();
		setMemSize(memSizeOpt.get(opt - 1));
		for(int i = 1; i <= pageSizeOpt.size(); ++i) {
			System.out.print(i + ". " + pageSizeOpt.get(i - 1) + "KB  ");
		}
		System.out.println();
		opt = input.nextInt();
		setPageSize(pageSizeOpt.get(opt - 1));
		initialized = 1;	// 初始化完成
	}
}

public class BuddyHeap {
	private int NUM;
	private List<Integer> alloSizeOpt;
	private int memSize;
	private int pageSize;
	
	private ArrayList<LinkedList<Integer>> freeArea;	// 长度为NUM，每项中为空闲区的链表，初始时只有NUM-1位置有空闲区，数量为memSize/(2^(NUM-1) * pageSize)
	private ArrayList<ArrayList<Integer>> bitMap;		// 长度为NUM，每项中为位视图，位视图的长度为 memSize/(2^i*pageSize)/2，每位的初始值都为0
	
	public BuddyHeap(Config config) {
		// 根据config对buddy系统进行初始化
		freeArea = new ArrayList<LinkedList<Integer>>();
		bitMap = new ArrayList<ArrayList<Integer>>();
		System.out.println(freeArea.size());
		// 初始化内存大小，页框大小
		alloSizeOpt = config.getAlloSizeOpt();
		memSize = config.getMemSize();
		pageSize = config.getPageSize();
		NUM = config.getNUM();
		// 初始化freeArea
		for(int i = 0; i < NUM; ++i) {
			freeArea.add(new LinkedList<Integer>());
			bitMap.add(new ArrayList<Integer>());
		}
		for(int i = 0; i < memSize * 1024; i += alloSizeOpt.get(NUM - 1) * pageSize) {
			freeArea.get(NUM - 1).add(i / pageSize);	// 将空闲区首地址填入到空闲链表中
		}
		// 初始化bitMap
		for(int i = 0; i < bitMap.size(); ++i) {
			int k = (memSize * 512) / (alloSizeOpt.get(i) * pageSize);	// 计算每个块组对应的位视图的长度
			for(int j = 0; j < k; ++j) {
				bitMap.get(i).add(0);
			}
		}
	}
	public void showFreeArea() {
		// 输出空闲区内容
		for(int i = 0; i < NUM; ++i) {
			System.out.print(i + "(" + alloSizeOpt.get(i) + ")" + "  ");
			if(freeArea.get(i).isEmpty()) {
				System.out.println("null");
				continue;
			}
			for(int j = 0; j < freeArea.get(i).size() - 1; ++j) {
				System.out.print(freeArea.get(i).get(j) + "->");
			}
			System.out.println(freeArea.get(i).get(freeArea.get(i).size() - 1));
		}
	}
	public void showBitMap() {
		for(int i = 0; i < bitMap.size(); ++i) {
			System.out.print(i + "  ");
			for(int j = 0; j < bitMap.get(i).size(); ++j) {
				System.out.print(bitMap.get(i).get(j) + " ");
			}
			System.out.println();
		}
	}
	public void changeBitMap(int pageNum, int pos) {
		// 输入pageNum分配的首页框，pos表示在freeArea[pos]处分配的空闲区空间
		System.out.println("pageNum = " + pageNum + " pos = " + pos);
		int bgPos = pageNum >> (pos + 1); 		// bgPos表示对应在bitMap[pos]中的位置，用于判断是否能和伙伴形成空闲区或申请空闲区导致bitMap修改
		if(bitMap.get(pos).get(bgPos) == 0) {
			// 由0到1
			bitMap.get(pos).set(bgPos, 1);
			pos += 1;
			bgPos >>= 1;
			while(pos < NUM && bitMap.get(pos).get(bgPos) == 0) {
				bitMap.get(pos).set(bgPos, 1);
				bgPos >>= 1;
				pos += 1;
			}
			if(pos < NUM) {
				bitMap.get(pos).set(bgPos, 0);
			}
		}
		else {
			// 由1到0
			bitMap.get(pos).set(bgPos, 0);
		}
//		// 修改伙伴位图中低位的值
//		int tmpPos = 0;
//		int pageNumL = pageNum;								// pageNumL表示申请的首个页面号
//		int pageNumR = pageNum + alloSizeOpt.get(pos) - 1;	// pageNumR表示申请的最后一个页面号
//		int tmpbgPosL = pageNumL >> 1;				// tmpbgPosL表示首个页面号对应在tmpPos的伙伴位图中的位置
//		int tmpbgPosR = pageNumR >> 1;				
//		while(tmpPos < pos) {
//			for(int i = tmpbgPosL; i <= tmpbgPosR; ++i) {
//				bitMap.get(tmpPos).set(i, 0);
//			}
//			tmpPos += 1;
//			tmpbgPosL >>= 1;
//			tmpbgPosR >>= 1;
//		}
	}
	public void malloc(int size) {
		// size表示申请的页数
		int pos = 0;
		int val = 1;
		while(val < size) {
			val *= 2;
			pos += 1;
		}
		// pos为申请内存应该分配的对应在空闲区链表组的位置，pos=1表示分配2个页框
		if(pos >= NUM) {
			System.out.println("超出最大可分配空间");
			return ;
		}
		if(freeArea.get(pos).size() > 0) {
			// 对应位置有空闲区，则直接分配
			int pageNum = freeArea.get(pos).get(0);	// pageNum表示分配的第一个页框号，分配2^pos(alloSizeOpt.get(pos))个页框
			freeArea.get(pos).remove(0);
			changeBitMap(pageNum, pos);
		}
		else {
			// 对应位置没有空闲区，则寻找上级链表，直到找到能分配的位置
			int pos2 = pos;	// pos2保存pos的值
			int pos3 = pos2; // 保存pos的值，因为后面pos2由于逐步给几个链表分配空闲区，所以会发生改变，而修改bitMap还需要原来的pos2的值
			while(pos < NUM && freeArea.get(pos).size() == 0) ++pos;
			if(pos >= NUM) {
				System.out.println("空闲空间不足");
				return ;
			}
			int pageNum = freeArea.get(pos).get(0);	// 分配首页框
			int tmpPageNum = pageNum;
			tmpPageNum += alloSizeOpt.get(pos2);	// 将首页框分配给请求进程，页框数为alloSizeOpt.get(ppos)
			freeArea.get(pos).remove(0);
			while(pos2 < pos) {
				freeArea.get(pos2).add(tmpPageNum); // 向空闲链中添加对的首地址，如请求256，分配2048个页框，最开始的256个被分配，之后的空间依次插入到空闲链表中
				tmpPageNum += alloSizeOpt.get(pos2);
				pos2 += 1;
			}
			changeBitMap(pageNum, pos3);
		}
	}
	public void free(int pageNum) {
		// 释放pageNum号页框空间
		// 释放空间从伙伴块组0位置开始向上修改bitMap
		int bgPos = pageNum >> 1;
		int pos = 0;	
		int val = 2;
		while(pos < NUM && bitMap.get(pos).get(bgPos) == 1) {
			// 由1到0则向上生成空闲区
			bitMap.get(pos).set(bgPos, 0);
			pageNum = val * bgPos; // 每组空闲区的首页号 = 2^(pos + 1) * bgPos
			val *= 2;
			pos += 1;
			bgPos >>= 1;
		}
		if(pos != NUM) {
			// pos为最后生成的空闲区应该插入的位置
			// pageNum为最后生成的空闲区中首页框号
			bitMap.get(pos).set(bgPos, 1);
			freeArea.get(pos).add(pageNum);
		}
		else {
			// 已经生成了最大容量的空闲区，值
			freeArea.get(pos - 1).add(pageNum);
		}
	}
	public static void main(String[] args) {
		System.out.println("你好");
		Config config = new Config();
		config.init();
		BuddyHeap bh = new BuddyHeap(config);
		bh.showFreeArea();
		System.out.println();
		bh.showBitMap();
		while(true) {
			System.out.println("1. 申请  2. 释放  3.  退出");
			int opt = (new Scanner(System.in)).nextInt();
			if(opt == 1) {
				System.out.print("输入申请的空间大小:");
				int size = (new Scanner(System.in)).nextInt();
				bh.malloc(size);
				bh.showFreeArea();
				System.out.println();
				bh.showBitMap();
			}
			else if(opt == 2){
				System.out.print("输入释放页框号:");
				int pageNum = (new Scanner(System.in)).nextInt();;
				bh.free(pageNum);
				bh.showFreeArea();
				System.out.println();
				bh.showBitMap();
			}
			else{
				break;
			}
		}
	}
}
