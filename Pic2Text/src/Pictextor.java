import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.awt.Toolkit;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import javax.imageio.ImageIO;

import main.java.at.dhyan.open_imaging.GifDecoder;
import main.java.at.dhyan.open_imaging.GifDecoder.GifImage;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class Pictextor {

	private static final int BOXSIZE = 10;

	/**
	 * generates a grayscale pixelated image from grayscale image
	 * sized (width/boxsize,height/boxsize)
	 *
	 * @param boxsize
	 * 				int size of rectangular area containing one character
	 * @param image
	 * 				BufferedImage grayscale image
	 * @return grayscale bufferedimage where each pixel represents a rectangular area in original image
	 */
	private static BufferedImage GrayImgBoxing(BufferedImage image, int boxsize)
	{
		int sumred=0;
		int rgbvalue=0;
		int red=0;
		int tempbox=0;
		int newWidth=image.getWidth()/boxsize;
		int newHeight=image.getHeight()/boxsize;
		BufferedImage boxed = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_BYTE_GRAY);
		for (int i=0;i<newWidth;i++)
		{
			for (int j=0;j<newHeight;j++)
			{
				sumred=0;
				for (int x=0;x<boxsize;x++)
				{
					for (int y=0;y<boxsize;y++)
					{
						rgbvalue=image.getRGB(i*boxsize+x,j*boxsize+y);
						red =  (rgbvalue >> 16 ) & 0xFF;     // red component
						sumred+=red;
					}
				}
				sumred = sumred/(boxsize*boxsize); 
				tempbox= (sumred << 16) + (sumred<<8) + (sumred); // so that boxed is gray even if type byte rgb
				boxed.setRGB(i,j,tempbox);
			}
		}
		return boxed;
	}

	/**
	 * calculates gryascale brightness of a ARGB int
	 *
	 * @param rgbint
	 * 				integer RGB value of pixel
	 * 
	 * @return integer value of grayscale brightness (alpha channel unchanged)
	 */
	private static int RGBtoGray(int rgbint)  
	{
		double temp=0.0;
		int intgray=0;
	  	int alpha = (rgbint >> 24) & 0xFF;
		int red =   (rgbint >> 16) & 0xFF;
		int green = (rgbint >>  8) & 0xFF;
		int blue =  (rgbint      ) & 0xFF;
		temp = (red*0.299 + green*0.587 + blue*0.114) ; 
	/*	temp = (red + green + blue)/3.0; is also valid  */
		intgray = (int)(temp);
		int gray =(alpha >> 24)+ (intgray << 16) + (intgray<<8) + (intgray);
		return gray;
	}
	
	/**
	 * generates a grayscale BufferedImage from a colored image
	 * 
	 * @param color
	 * 				colored BufferedImage
	 * 
	 * @return TYPE_BYTE_GRAY BufferedImage with same dimensionss
	 */
	private static BufferedImage ImageToGray(BufferedImage color)
	{
		int width = color.getWidth();
		int height= color.getHeight();
		int colorPixel=0;
		int grayPixel=0;
		BufferedImage gray = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		for (int i = 0; i<width ; i++)
		{
			for (int j=0; j<height ; j++)
			{
				colorPixel=color.getRGB(i,j);
				grayPixel=RGBtoGray(colorPixel);
				gray.setRGB(i,j,grayPixel);
			}
		}
		return gray;
	}
	
	/**
	 * calculate a character relative brightness (or "darkness")
	 * 
	 * @param letter
	 * 				String value of character (length==1)
	 * @param boxsize
	 * 				int size of rectangular area containing one character
	 * @param isbold
	 * 				boolean true if letter is bold false if regular
	 * 
	 * @return integer value of relative brightness
	 */
	public static int CalcLetterBrightness(String letter,int boxsize, boolean isbold)
	{
		int sumblue=0;
		int rgbvalue=0;
		int blue=0;
		BufferedImage tempimg = new BufferedImage(boxsize*2,boxsize*2 , BufferedImage.TYPE_4BYTE_ABGR);
	    Graphics2D tempgraphics = tempimg.createGraphics();
	    double fontSize= 6 + boxsize * Toolkit.getDefaultToolkit().getScreenResolution() / 72.0;
	    if (isbold)
	    {
	    	tempgraphics.setFont(new Font("monospaced", Font.BOLD, (int)fontSize));
	    }
	    else
	    {
	    	tempgraphics.setFont(new Font("monospaced", Font.PLAIN, (int)fontSize));
	    }
	    tempgraphics.setColor(Color.BLACK);     		    
     	tempgraphics.drawString(letter,0, boxsize);    
     	 	
     	// all colored pixels should be black or white so r=g=b and there is no need for R,G vlaues
     	// type byte gray considers only the red component (and alpha?)
		for (int i=0;i<boxsize;i++)
		{
			for (int j=0;j<boxsize;j++)
			{				
						rgbvalue = tempimg.getRGB(i,j);
						blue =  (rgbvalue  >>  24  ) & 0xFF;
						sumblue = sumblue + blue;					
			}
		}
     	return sumblue;
	}
	 
	/**
	 * create LetterPixel arraylist - list will be twice as long as stringchars, 
	 * every character will be added twice (bold/regular)
	 * 
	 * @param stringchars
	 * 				String[] array of character string values (length==1)
	 * @param boxsize
	 * 				int size of rectangular area containing one character
	 * 
	 * @return an arraylist of LetterPixels with normalized brightness (within range 0-255),
	 * sorted by brightness in ascending order
	 */
	public static ArrayList<LetterPixel> createLetterScale(String[] stringchars, int boxsize)
	{
		int length = Array.getLength(stringchars);
		int tempbright=0;
		
		ArrayList<LetterPixel> arraylist = new ArrayList<LetterPixel>();
		
		
		for(int i=0;i<length;i++)
		{
			
			tempbright = CalcLetterBrightness(stringchars[i], boxsize, false);
			arraylist.add(new LetterPixel(stringchars[i],tempbright , false));
			
			
		}
		for(int i=0;i<length;i++)
		{
			tempbright = CalcLetterBrightness(stringchars[i], boxsize, true);
			arraylist.add(new LetterPixel(stringchars[i],tempbright , true));
		}
		arraylist.sort(null);
		//Collections.sort(arraylist);
		arraylist=normalizeSortedScale(arraylist);
		
		/* printing of list check
		for (LetterPixel lp : arraylist)
		{
			System.out.println(lp.ToString());
		}
		 */
		
		return arraylist;
	}
	
	/**
	 * create LetterPixel ArrayList - list will be twice as long as charstring, 
	 * every character will be added twice (bold/regular)
	 * 
	 * @param charstring
	 * 				String of all characters to be added to list
	 * @param boxsize
	 * 				int size of rectangular area containing one character
	 * 
	 * @return ArrayList of LetterPixels with normalized brightness (within range 0-255),
	 * sorted by brightness in ascending order
	 */
	public static ArrayList<LetterPixel> createLetterScale(String charstring, int boxsize)
	{
		int length = charstring.length();
		int tempbright=0;
		
		ArrayList<LetterPixel> arraylist = new ArrayList<LetterPixel>();
		
		for(int i=0;i<length;i++)
		{
			
			tempbright = CalcLetterBrightness(String.valueOf(charstring.charAt(i)), boxsize, false);
			arraylist.add(new LetterPixel(String.valueOf(charstring.charAt(i)),tempbright , false));
			
			
		}
		for(int i=0;i<length;i++)
		{
			tempbright = CalcLetterBrightness(String.valueOf(charstring.charAt(i)), boxsize, true);
			arraylist.add(new LetterPixel(String.valueOf(charstring.charAt(i)),tempbright , true));
		}
		
		Collections.sort(arraylist);
		arraylist=normalizeSortedScale(arraylist);
		
		/* printing of list check
		for (LetterPixel lp : arraylist)
		{
			System.out.println(lp.ToString());
		}
		 */
		
		return arraylist;
	}
		
	/**
	 * normalize a sorted LetterPixel ArrayList, brightness will be of range 0-255, order not changed
	 * 
	 * @param arraylist
	 * 				a LetterPixel ArrayList, sorted by brightness in ascending order
	 */
	public static void normalizeSortedArraylist (ArrayList<LetterPixel> arraylist)
	{
		// Daniel notes: Functions should have a return value and not mutate the object that was sent to them
		int tempbrightness=0;
		int index = arraylist.size()-1;
		LetterPixel temp = arraylist.get(index);
		int factor = temp.getBrightness();
		for (LetterPixel lp : arraylist)
		{
			tempbrightness = lp.getBrightness();
			lp.setBrightness(tempbrightness*255/factor);
		}
		
	}
	
	/**
	 * normalize a sorted LetterPixel ArrayList, brightness will be of range 0-255, order not changed
	 * 
	 * @param arraylist
	 * 				a LetterPixel ArrayList, sorted by brightness in ascending order
	 */
	public static ArrayList<LetterPixel> normalizeSortedScale (ArrayList<LetterPixel> arraylist)
	{
		// Daniel notes: Functions should have a return value and not mutate the object that was sent to them
		int tempbrightness=0;
		int index = arraylist.size()-1;
		ArrayList<LetterPixel> templist = arraylist;
		LetterPixel temp = arraylist.get(index);
		int factor = temp.getBrightness();
		for (LetterPixel lp : templist)
		{
			tempbrightness = lp.getBrightness();
			lp.setBrightness(tempbrightness*255/factor);
		}
		return templist;
	}
	
	/**
	 * match given brightness to a LetterPixel from a sorted,normalized ArrayList 
	 * 
	 * @param brightness
	 * 				int brightness value (should be in range 0-255)
	 * @param sortedlist
	 * 				ArrayList of LetterPixels with normalized brightness (within range 0-255),
	 * 				sorted by brightness in ascending order
	 * 
	 * @return int index of LetterPixel in sortedlist matching given brightness
	 */
	public static int indexOfBrightness(int brightness, ArrayList<LetterPixel> sortedlist )
	{
		int temp=0;
		LetterPixel comparedlp = new LetterPixel("o",brightness, false);
     	int index = Collections.binarySearch(sortedlist,comparedlp,null);
     	if (index<0) // find nearest neighbor
     	{
     		int delta1=sortedlist.get(-index-1).getBrightness()-brightness;
     		int delta2=brightness-sortedlist.get(-index-2).getBrightness();
     		if (delta1-delta2<0)
     		{
     			temp=-index-1;
     		}
     		else
     		{
     			temp=-index-2;
     		}
     	}
     	else
     	{
     		temp=index;
     	}
		return temp;
	}
	
	/**
	 * generate a letter-image from a "boxed" image 
	 * 
	 * @param boxed
	 * 				TYPE_BYTE_GRAY BufferedImage "boxed" grayscale version of original image
	 * @param sortedlist
	 * 				ArrayList of LetterPixels with normalized brightness (within range 0-255),
	 * 				sorted by brightness in ascending order
	 * @param boxsize
	 * 				int size of rectangular area containing one character
	 * 
	 * @return BufferedImage TYPE_3BYTE_BGR image painted with characters
	 */
	public static BufferedImage boxedToLetters (BufferedImage boxed , ArrayList<LetterPixel> sortedlist, int boxsize)
	{
		int width = boxed.getWidth();
		int height = boxed.getHeight();
		int tempbrightnessindex=5;
		int tempbright = 20;
		double fontSize= 6+ boxsize * Toolkit.getDefaultToolkit().getScreenResolution() / 72.0;
		BufferedImage tempimg = new BufferedImage(width*boxsize,height*boxsize , BufferedImage.TYPE_3BYTE_BGR);
	    Graphics2D tempgraphics = tempimg.createGraphics();
	    tempgraphics.setColor(Color.WHITE);  // this line and the next make sure background is white
	    tempgraphics.fillRect(0, 0, width*boxsize, height*boxsize);
	    tempgraphics.setColor(Color.BLACK);
	    Font boldFont = new Font("monospaced", Font.BOLD, (int)fontSize);
	    Font plainFont = new Font("monospaced", Font.PLAIN, (int)fontSize);
	    tempgraphics.setFont(plainFont);
     	
		for (int i=0;i<width;i++)
		{
			for (int j=0;j<height;j++)
			{	
				
				tempbright=boxed.getRGB(i,j);
				tempbright=(tempbright >> 16) & 0xFF;  // boxed - type_byte_gray considers only the red component
				tempbrightnessindex=indexOfBrightness(255-tempbright,sortedlist);
				if (sortedlist.get(tempbrightnessindex).getBold())
				{
					tempgraphics.setFont(boldFont);
				}
				else
				{
					tempgraphics.setFont(plainFont);
				}
				tempgraphics.drawString(sortedlist.get(tempbrightnessindex).getLetter(),i*boxsize, (j+1)*boxsize);   
			}
		}
		return tempimg;
	}
	
	/**
	 * generate an image painted with characters from input path
	 * 
	 * @param stringinput
	 * 				string of input file path (file is readable via ImageIO.read)
	 * @param stringoutput
	 * 				string of output path
	 * @param sortedScale
	 * 				ArrayList of LetterPixels with normalized brightness (within range 0-255),
	 * 				sorted by brightness in ascending order
	 * @param boxsize
	 * 				int size of rectangular area containing one character
	 */
	public static void ToLetterImage (String stringinput ,String stringoutput, ArrayList<LetterPixel> sortedScale, int boxsize){
        BufferedImage inputimg = null;
		try {
			inputimg = ImageIO.read(new File(stringinput));
			BufferedImage grayimg	= ImageToGray(inputimg);      
	        BufferedImage boxedimg 	= GrayImgBoxing(grayimg,boxsize);
	        BufferedImage letterimg = boxedToLetters(boxedimg, sortedScale,boxsize);
	        File outputimg = new File(stringoutput);
	        ImageIO.write(letterimg, "png", outputimg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("error");
		}

	}
	
	/**
	 * generate a .gif painted with characters from input path
	 * 
	 * @param inputpath
	 * 				string of input file path (file is readable via ImageIO.read)
	 * @param outputpath
	 * 				string of output path
	 * @param sortedScale
	 * 				ArrayList of LetterPixels with normalized brightness (within range 0-255),
	 * 				sorted by brightness in ascending order
	 * @param boxsize
	 * 				int size of rectangular area containing one character
	 */
	public static void ToLetterGif(String inputpath, String outputpath,ArrayList<LetterPixel> sortedScale, int boxsize ) throws Exception 
	{
		System.out.println("gif start");
		FileInputStream data = new FileInputStream(inputpath);
		final GifImage gif = GifDecoder.read(data); //get gif properties with dhyan blum encoder
	//	final int width = gif.getWidth();
	//	final int height = gif.getHeight();
	//	final int background = gif.getBackgroundColor();
		final int frameCount = gif.getFrameCount();
		FileOutputStream outputfile = new FileOutputStream (outputpath);
    	AnimatedGifEncoder e = new AnimatedGifEncoder(); //initialize gif encoder
    	e.start(outputfile);
    	e.setRepeat(0);
    	e.setQuality(12); 	// set quality/speed ratio indicator
		for (int i = 0; i < frameCount; i++) {
			BufferedImage gifimg = gif.getFrame(i);
			int delay = gif.getDelay(i);
			BufferedImage tempgifimg = gifimg;			// these 3 lines make sure background is white
			Graphics2D g = tempgifimg.createGraphics();				        
			g.drawImage(gifimg, 0, 0, Color.WHITE, null);			
			BufferedImage graygif = ImageToGray(tempgifimg);	        
			BufferedImage boxedgif = GrayImgBoxing(graygif,boxsize);
			BufferedImage lettergif = boxedToLetters(boxedgif, sortedScale,boxsize);
			e.setDelay(delay*10);
		  	e.addFrame(lettergif);
		}
		e.finish();
		System.out.println("gif end");
	}
	

	
	// main
	public static void main(String[] args) throws IOException
	{
       
    	              
        //create normalize sorted scale 
        
        ArrayList<LetterPixel> sortedScale = new ArrayList<LetterPixel>();
        String[] scaleChars = new String[]{"א", "ב", "ג", "ד", "ה","ו", "ז", "ח", "ט", "י"
				 ,"כ", "מ", "נ", "ס", "פ", "צ", "ר", "ש", "ת", "."
				 ," ", "-", "=", "+", ":", "ם"};
        sortedScale=createLetterScale(scaleChars,BOXSIZE);
         
        
        // print letter image
        /*
        String inputmonroe = "C:\\Users\\user\\Pic2TextWorkspace\\input\\talya.jpg";
        String outputmonroe = "C:\\Users\\user\\Pic2TextWorkspace\\output\\lettertalya.jpg";
        ToLetterImage(inputmonroe,outputmonroe,sortedScale,BOXSIZE);
		
        inputmonroe = "C:\\Users\\user\\Pic2TextWorkspace\\input\\daniel.jpg";
        outputmonroe = "C:\\Users\\user\\Pic2TextWorkspace\\output\\letterdaniel.jpg";
        ToLetterImage(inputmonroe,outputmonroe,sortedScale,BOXSIZE);
        */
        
        // create letter gif
             
		String inputgif = "C:\\Users\\user\\Pic2TextWorkspace\\DhyanB-gifdecoder\\src\\test\\resources\\input-images\\mario.gif";
  		String outputgif = "C:\\Users\\user\\Pic2TextWorkspace\\output\\giftest1.gif";
		try {
			ToLetterGif(inputgif, outputgif, sortedScale, BOXSIZE);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 
		  

	}
	
}
