package tinygp;

public class App 
{
    public static void main(String[] args) {
        String fileName = "examples/second1.txt";
        long seed = -1;

        if ( args.length == 2 ) {
            seed = Integer.parseInt(args[0]);
            fileName = args[1];
        }
        if ( args.length == 1 ) {
            fileName = args[0];
        }

        TinyGP gp = new TinyGP(fileName, seed);
        gp.evolve();
    }
}
