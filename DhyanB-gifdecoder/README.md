Open Imaging
============

Welcome to Open Imaging. Currently this project only contains a GIF decoder. At a later point, other tools and libraries that deal with the creation and processing of images may be added.

## GIF Decoder

A decoder capable of processing a GIF data stream to render the graphics contained in it. This implementation follows the official <A HREF="http://www.w3.org/Graphics/GIF/spec-gif89a.txt">GIF specification</A>.

### Example Usage
```java
void example(final byte[] data) throws Exception {
	final GifImage gif = GifDecoder.read(data);
	final int width = gif.getWidth();
	final int height = gif.getHeight();
	final int background = gif.getBackgroundColor();
	final int frameCount = gif.getFrameCount();
	for (int i = 0; i < frameCount; i++) {
		final BufferedImage img = gif.getFrame(i);
		final int delay = gif.getDelay(i);
		ImageIO.write(img, "png", new File(OUT_FOLDER + "frame_" + i + ".png"));
	}
}
```

You can also read from an input stream, though it will be converted to a byte array internally:

```java
	final FileInputStream data = new FileInputStream(IN_FOLDER + "some.gif");
	final GifImage gif = GifDecoder.read(data);
```

### Compatibility

* Support for GIF87a, GIF89a, animation, transparency and interlacing.
* Independent from third party libraries. Just download `GifDecoder.java` and the `LICENSE` file.
* Some GIF images cause an <a href="http://stackoverflow.com/questions/22259714/arrayindexoutofboundsexception-4096-while-reading-gif-file">ArrayIndexOutOfBoundsException: 4096</a> when using Java's official `ImageIO.read` method or the decoder used in Apache Imaging. <a href="http://www.fmsware.com/stuff/gif.html">Kevin Weiner's decoder</a> will either throw the same exception or render the frames of these images incorrectly. This decoder does not suffer from this bug.
* Requires Java 7.

### Performance

During development, this decoder has been frequently compared with the one from Kevin Weiner, which is well crafted and delivers high performance. I worked hard to deliver similar speed and current testing indicates that the decoder is about 15% faster than Kevin Weiner's:

	RESULTS FOR OPEN IMAGING DECODER
	Files: 26
	Repetitions: 100
	Total time: 142682 ms
	Time per repetition: 1426 ms

	RESULTS FOR KEVIN WEINER DECODER
	Files: 26
	Repetitions: 100
	Runtime: 167689 ms
	Time per repetition: 1676 ms

However, performance heavily depends on the set of images used for testing (see next paragraph) and the main motivation behind the development of this decoder wasn't speed but rather correctness. So I wouldn't insist in being faster, I just think the decoder delivers decent performance.

Either way, feel free to run your own tests! Any feedback is highly appreciated. A basic JUnit test comes with the package. Open `GifDecoderTest.java`, set `LOOPS` to a reasonable value and start the test. When `LOOPS` is set to 100, the first two test methods will let both decoders repeatedly create a buffered image for every single frame of every test image 100 times in a row.

There is also a third test method that will decode a single image and write its frames to `/src/test/resources/output-frames/`. This is a short test I run frequently after changing the code to ensure correctness.

### Images used during testing

The current testing set (see `/src/test/resources/input-images/`) consists of 26 different GIF images of various file sizes, image dimensions and other special characteristics. The biggest one is 5 MB (`space.gif`), the smallest one is only 69 bytes (`sample.gif`), all together sum up to 29.3 MB. All but three are animated GIFs. Some have transparent backgrounds, some have optimized frames with smaller dimensions than the base canvas. `bubble.gif` consists of 255 frames. Some images use interlacing (e.g. `hand.gif`). Three images cause the mentioned `ArrayOutOfBoundsException` in various other decoders. `fish.gif` does not have a trailer byte. `c64.gif` has a truncated `end of information` code at the end of the second frame. `train.gif` has a truncated image data sub-block at the end of the last frame. The last 5% of the data in `science.gif` is corrupted and cannot be restored.

### Quirks

<b>Background color:</b> If a GIF frame requires the next frame to be drawn on the background, a decoder would have to clear the canvas and restore the background color that is specified in the GIF's logical screen descriptor. Many GIFs that look like they should actually have a transparent background would then have an opaque background. Therefore this decoder only sets the canvas to the background color, if the next frame has no transparent color defined. Otherwise, a transparent background will be used to draw upon. Testing indicates that this approach works fine. However, you can still ask the decoder for the background color of the first frame and use it to set the background of buffered images on your own.

### Support Open Imaging

If you feel like this project deserves a donation, checkout my Pledgie button :-)

<a href='https://pledgie.com/campaigns/26861'><img alt='Click here to lend your support to: GIF Image Decoder and make a donation at pledgie.com !' src='https://pledgie.com/campaigns/26861.png?skin_name=chrome' border='0' ></a>