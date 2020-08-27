public class SingleTon {
    private static volatile SingleTon singleTon=null;
    private SingleTon(){
    }
    public static SingleTon test(){
        if(singleTon==null){
            synchronized (SingleTon.class){
                if(singleTon==null){
                    singleTon=new SingleTon();
                }
            }
        }
        return singleTon;
    }
}
