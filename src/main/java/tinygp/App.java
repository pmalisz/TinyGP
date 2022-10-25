package tinygp;

public class App 
{
    public static void main(String[] args) {
        String fileName = "examples/ex6.txt";
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
