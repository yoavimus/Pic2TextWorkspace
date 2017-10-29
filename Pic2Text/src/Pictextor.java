import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.awt.Image;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;


import main.java.at.dhyan.open_imaging.GifDecoder;
import main.java.at.dhyan.open_imaging.GifDecoder.GifImage;




public class Pictextor {

	private static final int TYPE_BYTE_GRAY = 0;

	// gets a grayscale bufferedimage and returns a "boxed" version (avg over 16*16 boxes)
	// if image is in standard rgb result will still be "boxed" grayscale of red component
	//width and height are devisible by 16
	private static BufferedImage GrayImgBoxing(BufferedImage image)
	{
		int sumred=0;
		int rgbvalue=0;
		int red=0;
		int tempbox=0;
		int newWidth=image.getWidth()/16;
		int newHeight=image.getHeight()/16;
		BufferedImage boxed = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_BYTE_GRAY);
		for (int i=0;i<newWidth;i++)
		{
			for (int j=0;j<newHeight;j++)
			{
				sumred=0;
				for (int x=0;x<16;x++)
				{
					for (int y=0;y<16;y++)
					{
						rgbvalue=image.getRGB(i*16+x,j*16+y);
						red =  (rgbvalue >> 16 ) & 0xFF;     // red component
						sumred+=red;
					}
				}
				sumred = sumred/256; 
				tempbox= (sumred << 16) + (sumred<<8) + (sumred); // so that boxed is gray even if type byte rgb
				boxed.setRGB(i,j,tempbox);
			}
		}
		return boxed;
	}

	// will return int value of pixel(0-255) in grayscale
	// rgbint is a int representing rgb pixel value
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
		int gray = (intgray << 16) + (intgray<<8) + (intgray);
		return gray;
	}
	
	// get a AGBR bufferedimage and returns it in grayscale (0-255)
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
	
	//will return 2D pixel array of BufferedImage - currently not used
	@SuppressWarnings("unused")
	private static int[][] ImageToArray(BufferedImage image) 
	{
	      int width = image.getWidth();
	      int height = image.getHeight();
	      int[][] result = new int[height][width];

	      for (int row = 0; row < height; row++) 
	      {
	         for (int col = 0; col < width; col++) 
	         {
	            result[row][col] = RGBtoGray(image.getRGB(col, row));
	         }
	      }

	      return result;
	}
		
	// draw char scaling	- currently not needed
	private static void drawCharacter(int x, int y, char c,CharacterImageGenerator characterGenerator,Graphics graphics ) {
		int PADDING =0;
		int TILE_WIDTH = 16;
		int TILE_HEIGHT = 16;
 	    graphics.drawImage(characterGenerator.getImage(c), PADDING + (x * TILE_WIDTH), 
 	    			PADDING + (y * TILE_HEIGHT), TILE_WIDTH, TILE_HEIGHT, null);
 	}
	
	//calculate font size for a box size	
	public static int CalculateFontSize (int boxsize)
	{
		int temp=0;
		int maxheightindex=0;
		int maxwidthindex=0;
		int maxheight=0;
		int maxwidth=0;
		
		BufferedImage tempimg = new BufferedImage(boxsize*2,boxsize*2 , BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D tempgraphics = tempimg.createGraphics();		
		FontMetrics metrics = tempgraphics.getFontMetrics();
		tempgraphics.setFont(new Font("monospaced", Font.BOLD, 28));
		
		String[] stringchars = new String[]{"א", "ב", "ג", "ד", "ה","ו", "ז", "ח", "ט", "י"
				 ,"כ", "מ", "נ", "ס", "פ", "צ", "ר", "ש", "ת", "."
				 ," ", "-", "=", "+", ":", "ם"};
		for (int i=0;i<stringchars.length;i++)
		{
			Rectangle2D bounds = metrics.getStringBounds(stringchars[i], tempgraphics);
			if ((int)bounds.getWidth()>maxwidth){
				maxwidthindex=i; 
				maxwidth=(int)bounds.getWidth();
			}
			if ((int)bounds.getHeight()>maxheight){
				maxheightindex=i; 
				maxheight=(int)bounds.getHeight();
			}
		}
		for (int j=6;j<71;j=j+2)
		{
			tempgraphics.setFont(new Font("monospaced", Font.BOLD, j));
			Rectangle2D bounds1 = metrics.getStringBounds(stringchars[maxheightindex], tempgraphics);
			Rectangle2D bounds2 = metrics.getStringBounds(stringchars[maxwidthindex], tempgraphics);
			if(bounds1.getHeight()>boxsize)
			{
				break;
			}
			if(bounds2.getWidth()>boxsize)
			{
				break;
			}
			temp=j;
		}
			
		return temp;
	}
	
	// calculate brightness of a letter(character to string)	
	public static int CalcLetterBrightness(String letter,int boxsize, boolean isbold)
	{
		int sumblue=0;
		int rgbvalue=0;
		int blue=0;
		BufferedImage tempimg = new BufferedImage(boxsize*2,boxsize*2 , BufferedImage.TYPE_4BYTE_ABGR);
	    Graphics2D tempgraphics = tempimg.createGraphics();
	    if (isbold)
	    {
	    	tempgraphics.setFont(new Font("monospaced", Font.BOLD, 28));
	    }
	    else
	    {
	    	tempgraphics.setFont(new Font("monospaced", Font.PLAIN, 28));
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
    
	// testing printing letters all-over	- not needed(but for template purposes) working
	public static BufferedImage printletters (String letter )
	{

		BufferedImage tempimg = new BufferedImage(512,512 , BufferedImage.TYPE_4BYTE_ABGR);
	    Graphics2D tempgraphics = tempimg.createGraphics();
	    tempgraphics.setColor(Color.BLACK);     		       
     	tempgraphics.setFont(new Font("monospaced", Font.BOLD, 28));
     	// all colored pixels should be black or white so r=g=b and there is no need for R,G vlaues
		for (int i=0;i<32;i++)
		{
			for (int j=0;j<32;j++)
			{				
				tempgraphics.drawString(letter,i*16, (j+1)*16);   
			}
		}
		return tempimg;
	}
	
	// creating and printing a brightness scale	
	public static void printscale ()
	{
		String[] stringchars = new String[]{"א", "ב", "ג", "ד", "ה","ו", "ז", "ח", "ט", "י"
									 ,"כ", "מ", "נ", "ס", "פ", "צ", "ר", "ש", "ת", "."
									 ," ", "-", "=", "+", ":", "ם"};
		int[] scale = new int[stringchars.length*2];
		
		for (int i=0 ; i<stringchars.length ; i++)
		{
			scale[i]=CalcLetterBrightness(stringchars[i], 16, false);
			scale[i+stringchars.length]=CalcLetterBrightness(stringchars[i], 16, true);
		}
		System.out.println(Arrays.toString(scale));
	}
	
	// create LetterPixel arraylist - list will be twice as long as stringchars
	public static ArrayList<LetterPixel> createLetterScale(String[] stringchars)
	{
		int length=0;
		length = Array.getLength(stringchars);
		int tempbright=0;
		
		ArrayList<LetterPixel> arraylist = new ArrayList<LetterPixel>();
		
		
		for(int i=0;i<length;i++)
		{
			
			tempbright = CalcLetterBrightness(stringchars[i], 16, false);
			arraylist.add(new LetterPixel(stringchars[i],tempbright , false));
			
			
		}
		for(int i=0;i<length;i++)
		{
			tempbright = CalcLetterBrightness(stringchars[i], 16, true);
			arraylist.add(new LetterPixel(stringchars[i],tempbright , true));
		}
		
		Collections.sort(arraylist);
		normalizeSortedArraylist(arraylist);
		
		//temp printing of list check
		for (LetterPixel lp : arraylist)
		{
			System.out.println(lp.ToString());
		}
		
		return arraylist;
	}
	
	// normalize scale's letterpixels brightness to be of range 0-255
	public static void normalizeSortedArraylist (ArrayList<LetterPixel> arraylist)
	{
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
	
	// match brightness to a letter from a sorted letterpixel arraylist
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
	
	// try printing a picture from gray boxed bufferedimage
	public static BufferedImage boxedToLetters (BufferedImage boxed , ArrayList<LetterPixel> sortedlist )
	{
		int width = boxed.getWidth();
		int height = boxed.getHeight();
		int tempbrightnessindex=5;
		int tempbright = 20;
	/*	Comparator<LetterPixel> c = new Comparator<LetterPixel>()
        {
            public int compare(LetterPixel lp1, LetterPixel lp2)
            {
                return lp1.compareTo(lp2);
            }
        }; */
		BufferedImage tempimg = new BufferedImage(width*16,height*16 , BufferedImage.TYPE_4BYTE_ABGR);
	    Graphics2D tempgraphics = tempimg.createGraphics();
	    tempgraphics.setColor(Color.BLACK);
	    Font boldFont = new Font("monospaced", Font.BOLD, 28);
	    Font plainFont = new Font("monospaced", Font.PLAIN, 28);
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
				tempgraphics.drawString(sortedlist.get(tempbrightnessindex).getLetter(),i*16, (j+1)*16);   
			}
		}
		return tempimg;
	}
	
	
	/*----------------------------------------------------------------------------------------------------------------*/
	
	/* begin gif processing part*/
	
	
	void example(final FileInputStream data) throws Exception 
	{
		final GifImage gif = GifDecoder.read(data);
		final int width = gif.getWidth();
		final int height = gif.getHeight();
		final int background = gif.getBackgroundColor();
		final int frameCount = gif.getFrameCount();
		for (int i = 0; i < frameCount; i++) {
			final BufferedImage img = gif.getFrame(i);
			final int delay = gif.getDelay(i);
			ImageIO.write(img, "png", new File("C:\\Users\\user\\Pic2TextWorkspace\\output" + "frame_" + i + ".png"));
		}
	}
	
	
	// main
	public static void main(String[] args) throws IOException
	{
		BufferedImage img = null;
		BufferedImage img2 = null;
		try 
		{
			img = ImageIO.read(new File("C:\\Users\\user\\Pic2TextWorkspace\\Lenna.png"));
			
		} 
	
	/*comment */	
		
		
		catch (IOException e)
		{
		}
		
		BufferedImage grayimg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		
		grayimg=ImageToGray(img);
		
		File output = new File("C:\\Users\\user\\Pic2TextWorkspace\\grayLenna.png");
		
        ImageIO.write(grayimg, "png", output);
        
        BufferedImage boxedimg = new BufferedImage(grayimg.getWidth()/16, grayimg.getHeight()/16, BufferedImage.TYPE_BYTE_GRAY);
	
        boxedimg = GrayImgBoxing(grayimg);
        
        File output2 = new File("C:\\Users\\user\\Pic2TextWorkspace\\boxedLenna.png");
		
        ImageIO.write(boxedimg, "png", output2);
     	
         //print letter allover
         
         File output4 = new File("C:\\Users\\user\\Pic2TextWorkspace\\letter.png");
         
         ImageIO.write(printletters("א"), "png", output4);
         
         printscale();
         
         System.out.println(CalcLetterBrightness("ש", 32, false));
         
         
         //test normalized sorted scale printing
         ArrayList<LetterPixel> sortedScale = new ArrayList<LetterPixel>();
         String[] scaleChars = new String[]{"א", "ב", "ג", "ד", "ה","ו", "ז", "ח", "ט", "י"
				 ,"כ", "מ", "נ", "ס", "פ", "צ", "ר", "ש", "ת", "."
				 ," ", "-", "=", "+", ":", "ם"};
         sortedScale=createLetterScale(scaleChars);
         
       //match brightness 77 to a letter
         
         Comparator<LetterPixel> c = new Comparator<LetterPixel>()
         {
             public int compare(LetterPixel lp1, LetterPixel lp2)
             {
                 return lp1.compareTo(lp2);
             }
         }; 
     	LetterPixel comparedlp = new LetterPixel("o", 190, false);
     	int index = Collections.binarySearch(sortedScale,comparedlp,c);
     	System.out.println(index);
     	
     	// 	new new new try printing letter picture
        
        BufferedImage letterimg = new BufferedImage(boxedimg.getWidth()*16, boxedimg.getHeight()*16, BufferedImage.TYPE_4BYTE_ABGR);
        letterimg = boxedToLetters(boxedimg, sortedScale);
        File output7 = new File("C:\\Users\\user\\Pic2TextWorkspace\\letterlenna.png");
		
        ImageIO.write(letterimg, "png", output7);
        
        // try printing monroe
        
        img2 = ImageIO.read(new File("C:\\Users\\user\\Pic2TextWorkspace\\fart-spongebob.jpg"));
        BufferedImage letterimg2 = new BufferedImage(img2.getWidth(), img2.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
        BufferedImage grayimg2 = new BufferedImage(img2.getWidth(), img2.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        grayimg2=ImageToGray(img2);
        BufferedImage boxedimg2 = new BufferedImage(img2.getWidth()/16, img2.getHeight()/16, BufferedImage.TYPE_BYTE_GRAY);
        boxedimg2 = GrayImgBoxing(grayimg2);
        letterimg2 = boxedToLetters(boxedimg2, sortedScale);
        File output8 = new File("C:\\Users\\user\\Pic2TextWorkspace\\lettermonroe.png");
		
        ImageIO.write(letterimg2, "png", output8);
        
        int testintfont = CalculateFontSize(18);
        System.out.println(testintfont);
        
        /*---------------------------------start gif processing part------------------------------------------------------------------*/
        
        File filefile = new File("C:\\Users\\user\\Pic2TextWorkspace\\mario.gif");
    	FileInputStream data = new FileInputStream("C:\\Users\\user\\Pic2TextWorkspace\\DhyanB-gifdecoder\\src\\test\\resources\\input-images\\steps.gif");
    	GifImage gif = GifDecoder.read(data);
		int width = gif.getWidth();
		int height = gif.getHeight();
		int background = gif.getBackgroundColor();
		int frameCount = gif.getFrameCount();
		for (int i = 0; i < frameCount; i++) {
			BufferedImage gifimg = gif.getFrame(i);
			int delay = gif.getDelay(i);
			ImageIO.write(gifimg, "png", new File("C:\\Users\\user\\Pic2TextWorkspace\\output\\" + "frame_" + i + ".png"));
		}
	}
}
