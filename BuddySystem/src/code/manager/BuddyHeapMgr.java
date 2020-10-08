package code.manager;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

//
// 类名：BuddyHeapMgr
// 描述：管理伙伴堆
//
public class BuddyHeapMgr {
	private final int NUM;														// 表示空闲区链表的数量
	private final List<Integer> alloSizeOpt;									// 表示每个空闲表对应的空闲空间的页框数量

	public ArrayList<LinkedList<Integer>> getFreeArea() {
		return freeArea;
	}

	private final ArrayList<LinkedList<Integer>> freeArea;						// 长度为NUM，每项为空闲区的链表，初始时只有NUM-1位置有空闲区，数量为memSize/(2^(NUM-1) * pageSize)
	private final ArrayList<ArrayList<Integer>> bitMap;							// 长度为NUM，每项为位视图，位视图的长度为 memSize/(2^i*pageSize)/2，每位的初始值都为0

	private final ArrayList<Integer> tmpPageNum;								// 用于表示申请页的首地址
	private final ArrayList<Integer> tmpPageAmount;								// 用于表示申请页的数量

	// 描述：根据config初始化类中的属性
	public BuddyHeapMgr(Config config) {
		freeArea = new ArrayList<LinkedList<Integer>>();
		bitMap = new ArrayList<ArrayList<Integer>>();
		System.out.println(freeArea.size());
		alloSizeOpt = config.getAlloSizeOpt();
		int memSize = config.getMemSize();
		int pageSize = config.getPageSize();
		NUM = config.getNUM();
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

		tmpPageNum = new ArrayList<>();
		tmpPageAmount = new ArrayList<>();
	}

	// 描述：输出空闲区的信息，用链表的形式表示
	public void showFreeArea() {
		// 输出空闲区内容
		System.out.println("This is the information of Free Area: ");
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
	}         ///////////////////////////
	public String returnFreeArea() {
		// 输出空闲区内容
		String res = "";
		res += "This is the information of Free Area: \n";
		for(int i = 0; i < NUM; ++i) {
			res += i + "(" + alloSizeOpt.get(i) + ")" + "  :";
			if(freeArea.get(i).isEmpty()) {
				res += "null\n";
				continue;
			}
			for(int j = 0; j < freeArea.get(i).size() - 1; ++j) {
				res += freeArea.get(i).get(j) + "->";
			}
			res += freeArea.get(i).get(freeArea.get(i).size() - 1)+"\n";
		}
		return res;
	}

	// 描述：输出伙伴位视图的信息，用数组的形式表示
	private void showBitMap() {
		System.out.println("This is the information of Bit Map: ");
		for(int i = 0; i < bitMap.size(); ++i) {
			System.out.print(i + "  ");
			for(int j = 0; j < bitMap.get(i).size(); ++j) {
				System.out.print(bitMap.get(i).get(j) + " ");
			}
			System.out.println();
		}
	}

	// 描述：根据申请空间的首页框号和分配的页框数量（alloSizeOpt.get(pos)），修改伙伴位示图
	private void changeBitMap(int pageNum, int pos) {
		// 输入pageNum分配的首页框，pos表示在freeArea[pos]处分配的空闲区空间
//		System.out.println("pageNum = " + pageNum + " pos = " + pos);
		tmpPageNum.add(pageNum);
		tmpPageAmount.add(alloSizeOpt.get(pos)); 	// 向usedArea中添加项
		int bgPos = pageNum >> (pos + 1); 			// bgPos表示对应在bitMap[pos]中的位置，用于判断是否能和伙伴形成空闲区或申请空闲区导致bitMap修改
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
	}

	// 描述：申请size个页框
	private String malloc(int size) {    /////////////////
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
			return "超出最大可分配空间\n";
		}
		if(freeArea.get(pos).size() > 0) {
			// 对应位置有空闲区，则直接分配
			int pageNum = freeArea.get(pos).get(0);	// pageNum表示分配的第一个页框号，分配2^pos(alloSizeOpt.get(pos))个页框
			freeArea.get(pos).remove(0);
			System.out.println("分配首页框号：" + pageNum + "  分配页框数：" + alloSizeOpt.get(pos));
			changeBitMap(pageNum, pos);
			return "分配首页框号：" + pageNum + "  分配页框数：" + alloSizeOpt.get(pos)+"\n";
		}
		else {
			// 对应位置没有空闲区，则寻找上级链表，直到找到能分配的位置
			int pos2 = pos;	// pos2保存pos的值
			int pos3 = pos2; // 保存pos的值，因为后面pos2由于逐步给几个链表分配空闲区，所以会发生改变，而修改bitMap还需要原来的pos2的值
			while(pos < NUM && freeArea.get(pos).size() == 0) ++pos;
			if(pos >= NUM) {
				System.out.println("空闲空间不足");
				return "空闲空间不足\n";
			}
			int pageNum = freeArea.get(pos).get(0);	// 分配首页框
			int tmpPageNum = pageNum;
			tmpPageNum += alloSizeOpt.get(pos2);	// 将首页框分配给请求进程，页框数为alloSizeOpt.get(ppos)
			freeArea.get(pos).remove(0);
            System.out.println("分配首页框号：" + pageNum + "  分配页框数：" + alloSizeOpt.get(pos3));
            while(pos2 < pos) {
				freeArea.get(pos2).add(tmpPageNum); // 向空闲链中添加对的首地址，如请求256，分配2048个页框，最开始的256个被分配，之后的空间依次插入到空闲链表中
				tmpPageNum += alloSizeOpt.get(pos2);
				pos2 += 1;
			}
			changeBitMap(pageNum, pos3);
            return "分配首页框号：" + pageNum + "  分配页框数：" + alloSizeOpt.get(pos3)+"\n";
		}
	}

	// 描述：释放pageNum号页框空间
	private void free(int pageNum) {
        for(int i = 0; i < tmpPageNum.size(); ++i){
            if(pageNum >= tmpPageNum.get(i) && pageNum <= tmpPageNum.get(i) + tmpPageAmount.get(i) - 1){
                int lAmount = pageNum - tmpPageNum.get(i);
                int rAmount = tmpPageNum.get(i) + tmpPageAmount.get(i) - 1 - pageNum;
                int lBegin = tmpPageNum.get(i);
                int rBegin = pageNum + 1;
                tmpPageNum.remove(i);
                tmpPageAmount.remove(i);
                if(lAmount > 0) {
                    tmpPageNum.add(lBegin);
                    tmpPageAmount.add(lAmount);
                }
                if(rAmount > 0) {
                    tmpPageNum.add(rBegin);
                    tmpPageAmount.add(rAmount);
                }
                break;
            }
        }
		// 释放空间从伙伴块组0位置开始向上修改bitMap
		int bgPos = pageNum >> 1; // 0 + 1
		int pos = 0;	// pos为处理的伙伴位图的组号
		int val = 2;
		while(pos < NUM && bitMap.get(pos).get(bgPos) == 1) {
			bitMap.get(pos).set(bgPos, 0);
			pageNum = val * bgPos; // 每组空闲区的首页号 = 2^(pos + 1) * bgPos
			for(int i = 0; i < freeArea.get(pos).size(); ++i){
				// 如果生成了更大的空闲区，则移除小的空闲区
				if(freeArea.get(pos).get(i) == pageNum){
					freeArea.get(pos).remove(i);
					break;
				}
			}
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
			// 已经生成了最大容量的空闲区
			freeArea.get(pos - 1).add(pageNum);
		}
	}

	// 描述：随机申请一定数量的的页框
	public String randomAllocate() {  /////////////////////////
		String res = "";
		int size = (int)(Math.random() * 1.1 * alloSizeOpt.get(NUM - 1)) + 1;
		System.out.println("申请页框数: " + size);
		res += "申请页框数: " + size +" | " + malloc(size) + "\n";
		return res;
	}

	// 描述：随机释放某个页框
	public String randomFree() {/////////////////////////////
        if(tmpPageNum.isEmpty()) {
            System.out.println("没有能够释放的空间");
            return "没有能够释放的空间\n";
        }
        // 随机生成释放的页框号
        int index = (int)(Math.random() * tmpPageAmount.size());
        int pageNum = tmpPageNum.get(index) + (int)(Math.random() * tmpPageAmount.get(index));
        System.out.println("释放页框号：" + pageNum);
        free(pageNum);
        return "释放页框号：" + pageNum +"\n";
	}

	// 描述：初始化
	public void init() {
	    System.out.println("Initialized.");
    }

    // 描述：运行算法
	public void run() {
        showFreeArea();
        System.out.println();
        while(true) {
            System.out.println("1. 申请  2. 释放  3.  退出");
            int opt = (new Scanner(System.in)).nextInt();
            if(opt == 1) {
                randomAllocate();
                showFreeArea();
                System.out.println();
                showBitMap();
            }
            else if(opt == 2) {
                randomFree();
                showFreeArea();
                System.out.println();
                showBitMap();
            }
            else{
                break;
            }
        }
    }
	public String run(int opt) {
		String res = "";
			if(opt == 1) {
				res = randomAllocate();
				showFreeArea();
				System.out.println();
				showBitMap();
			}
			else if(opt == 2) {
				res = randomFree();
				showFreeArea();
				System.out.println();
				showBitMap();
			}
			return res;
	}

    // 描述：结束算法
    public void term() {
	    System.out.println("Bye");
    }

//	public static void main(String[] args) {   ///////////////////////////////////////////////////////////////
//		Config config = new Config();
//		config.init();
//		BuddyHeapMgr buddyHeapMgr = new BuddyHeapMgr(config);
//		buddyHeapMgr.init();
//		buddyHeapMgr.run();
//		buddyHeapMgr.term();
//	}

	//
	// 类名：Config
	// 描述：工厂类，用于配置BuddyHeapMgr的参数
	//
	public static class Config{      ////////////////////////////////////////////////////////
		private int initialized;
		private final int NUM = 10;
		private List<Integer> memSizeOpt;
		private List<Integer> pageSizeOpt;
		private final List<Integer> alloSizeOpt;
		private int memSize;
		private int pageSize;
		private final Scanner input;

		public Config() {
			input = new Scanner(System.in);
			initialized = 0;
			memSizeOpt = Arrays.asList(256, 512);	// MB
			pageSizeOpt = Arrays.asList(1, 2, 4, 256);	// KB
			alloSizeOpt = new ArrayList<>();	// 能够申请的内存大小
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
			System.out.print("Select page size: ");
			opt = input.nextInt();
			setPageSize(pageSizeOpt.get(opt - 1));
			initialized = 1;	// 初始化完成
		}
		public void init(int memOpt,int pageOpt) {////////////////////////////////////
			setMemSize(memSizeOpt.get(memOpt - 1));
			setPageSize(pageSizeOpt.get(pageOpt - 1));
			initialized = 1;	// 初始化完成
		}
	}
}
