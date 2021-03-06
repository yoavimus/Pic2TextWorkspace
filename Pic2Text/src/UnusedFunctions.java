import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.imageio.ImageIO;

public class UnusedFunctions {
	
	
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
	
	//calculate font size for a box size	- not working properly, not used
	public static int CalculateFontSize (int boxsize)
	{
		int temp=0;
		int maxheightindex=0;
		int maxwidthindex=0;
		double maxheight=0.0;
		double maxwidth=0.0;
		
		BufferedImage tempimg = new BufferedImage(boxsize*26,boxsize*2 , BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D tempgraphics = tempimg.createGraphics();		
		FontMetrics metrics = tempgraphics.getFontMetrics();
		Font font =new Font("monospaced", Font.BOLD, 28);
		tempgraphics.setFont(font);
		tempgraphics.setColor(Color.BLACK);     
	    FontRenderContext context = tempgraphics.getFontRenderContext();
	    
		String[] stringchars = new String[]{"א", "ב", "ג", "ד", "ה","ו", "ז", "ח", "ט", "י"
				 ,"כ", "מ", "נ", "ס", "פ", "צ", "ר", "ש", "ת", "."
				 ," ", "-", "=", "+", ":", "ם"};
		for (int i=0;i<stringchars.length;i++)
		{
			context = tempgraphics.getFontRenderContext();
			Rectangle2D bounds = font.getStringBounds(stringchars[i], context);
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
			context = tempgraphics.getFontRenderContext();
			Rectangle2D bounds1 = tempgraphics.getFont().getStringBounds(stringchars[maxheightindex], context);
			Rectangle2D bounds2 = tempgraphics.getFont().getStringBounds(stringchars[maxwidthindex], context);
			tempgraphics.drawString(stringchars[maxheightindex],(j-6)*16, boxsize*2);
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
		File tempoutput = new File("C:\\Users\\user\\Pic2TextWorkspace\\fonttest.png");
		
        try {
			ImageIO.write(tempimg, "png", tempoutput);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
		return temp;
	}
	
	// testing printing letters all-over	- not needed(but for template purposes) working
	public static BufferedImage printletters (String letter ){

			BufferedImage tempimg = new BufferedImage(512,512 , BufferedImage.TYPE_3BYTE_BGR);
		    Graphics2D tempgraphics = tempimg.createGraphics();
		    tempgraphics.setColor(Color.WHITE);
		    tempgraphics.fillRect(0, 0, 512, 512);
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
	public static void printscale (int boxsize)
	{
		String[] stringchars = new String[]{"א", "ב", "ג", "ד", "ה","ו", "ז", "ח", "ט", "י"
				,"כ", "מ", "נ", "ס", "פ", "צ", "ר", "ש", "ת", "."
				," ", "-", "=", "+", ":", "ם"};
		int[] scale = new int[stringchars.length*2];
			
		for (int i=0 ; i<stringchars.length ; i++)
		{
			scale[i]=CalcLetterBrightness(stringchars[i], BOXSIZE, false);
			scale[i+stringchars.length]=CalcLetterBrightness(stringchars[i], BOXSIZE, true);
		}
		System.out.println(Arrays.toString(scale));
	}
	
	
    /*---------------------------------start gif processing snippet------------------------------------------------------------------*/
    
    /*
    	System.out.print("begin gif part");
    	File filefile = new File("C:\\Users\\user\\Pic2TextWorkspace\\mario.gif");
		FileInputStream data = new FileInputStream("C:\\Users\\user\\Pic2TextWorkspace"
				+ "\\DhyanB-gifdecoder\\src\\test\\resources\\input-images\\mario.gif");
    	//FileInputStream data = new FileInputStream("C:\\Users\\user\\Pic2TextWorkspace\\giftest.gif");
		FileOutputStream output4 = new FileOutputStream ("C:\\Users\\user\\Pic2TextWorkspace\\giftest1.gif");
		AnimatedGifEncoder e = new AnimatedGifEncoder();
		e.start(output4);
		e.setRepeat(0);
		// e.setTransparent(Color.BLACK);
		e.setQuality(7);
		GifImage gif = GifDecoder.read(data);
		int width = gif.getWidth();
		int height = gif.getHeight();
		BufferedImage lettergif = null;
		int background = gif.getBackgroundColor();
		int frameCount = gif.getFrameCount();
		for (int i = 0; i < frameCount; i++) {
		BufferedImage gifimg = gif.getFrame(i);
		int delay = gif.getDelay(i);			
		//	ImageIO.write(gifimg, "png", new File("C:\\Users\\user\\Pic2TextWorkspace\\output\\" + "frame_" + i + ".png"));
		BufferedImage tempgifimg=gifimg;
		
		Graphics2D g = tempgifimg.createGraphics();				        
		g.drawImage(gifimg, 0, 0, Color.WHITE, null);
	    BufferedImage graygif=ImageToGray(tempgifimg);
	    BufferedImage boxedgif = GrayImgBoxing(graygif,BOXSIZE);
	    lettergif = boxedToLetters(boxedgif, sortedScale);		     
	  //ImageIO.write(lettergif, "png", new File("C:\\Users\\user\\Pic2TextWorkspace\\output\\" + "letter_frame_" + i + ".png"));		     
	  	e.setDelay(delay*10);
	  	e.addFrame(lettergif);
	}
	e.finish();
	System.out.print(" end gif part"); 
	*/
    /*------------------------------------------------------ end gif proccesing snippet-----------------*/
	

}
