package net.haspamelodica.javazmach.io.swt;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import net.haspamelodica.javazmach.GlobalConfig;
import net.haspamelodica.javazmach.core.io.VideoCard;
import net.haspamelodica.javazmach.core.text.UnicodeZSCIIConverter;
import net.haspamelodica.javazmach.core.text.ZSCIICharStream;

public class SWTVideoCard extends Canvas implements VideoCard
{
	private final UnicodeZSCIIConverter unicodeConv;

	private final Queue<Integer>	inputBuffer;
	private Image					screenBuffer;
	private GC						screenBufferGC;
	private Image					screenBuffer2;
	private GC						screenBuffer2GC;

	boolean debug = true;

	public SWTVideoCard(Composite parent, int style, GlobalConfig config)
	{
		super(parent, style | SWT.NO_BACKGROUND);

		this.unicodeConv = new UnicodeZSCIIConverter(config);
		this.inputBuffer = new ConcurrentLinkedQueue<>();
		this.screenBuffer = new Image(getDisplay(), 1, 1);
		this.screenBufferGC = new GC(screenBuffer);
		this.screenBuffer2 = new Image(getDisplay(), 1, 1);
		this.screenBuffer2GC = new GC(screenBuffer2);

		addListener(SWT.Resize, e ->
		{
			Point newSize = getSize();
			Rectangle oldBounds = screenBuffer.getBounds();
			if(newSize.x > oldBounds.width || newSize.y > oldBounds.height)
			{
				int newX;
				if(newSize.x > oldBounds.width)
					newX = newSize.x;
				else
					newX = oldBounds.width;
				int newY;
				if(newSize.y > oldBounds.height)
					newY = newSize.y;
				else
					newY = oldBounds.height;
				screenBufferGC.dispose();
				Image screenBufferOld = screenBuffer;
				screenBuffer = new Image(getDisplay(), newX, newY);
				screenBufferGC = new GC(screenBuffer);
				screenBufferGC.drawImage(screenBufferOld, 0, 0);
				screenBufferOld.dispose();
				this.screenBuffer2 = new Image(getDisplay(), newX, newY);
				this.screenBuffer2GC = new GC(screenBuffer2);
			}
		});
		addListener(SWT.KeyDown, e ->
		{
			if(e.character > 0)
			{
				int zscii = unicodeConv.unicodeToZSCII(e.character);
				if(zscii != -1)
					inputBuffer.offer(zscii);
			}
		});
		addPaintListener(e -> e.gc.drawImage(screenBuffer, 0, 0));
	}
	@Override
	public void dispose()
	{
		screenBuffer2GC.dispose();
		screenBuffer2.dispose();
		screenBufferGC.dispose();
		screenBuffer.dispose();
	}

	//VideoCard methods
	@Override
	public int getScreenWidth()
	{
		return isDisposed() ? -1 : execSWTSafe(() -> getSize().x);
	}
	@Override
	public int getScreenHeight()
	{
		return isDisposed() ? -1 : execSWTSafe(() -> getSize().y);
	}
	@Override
	public int getDefaultTrueFG()
	{
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public int getDefaultTrueBG()
	{
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public int getTrueColor(int color)
	{
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public int getCharWidth(int zsciiChar, int font, int style)
	{
		return isDisposed() ? -1 : execSWTSafe(() -> screenBufferGC.textExtent(String.valueOf(unicodeConv.zsciiToUnicodeNoNL(zsciiChar))).x);

		//TODO somehow this doesn't work?
		//return screenBufferGC.getCharWidth(unicodeConv.zsciiToUnicodeNoNL(zsciiChar));
	}
	@Override
	public int getFontHeight(int font)
	{
		return isDisposed() ? -1 : screenBufferGC.textExtent("0").y;

		//TODO somehow this doesn't work?
		//return screenBufferGC.getFont().getFontData()[0].getHeight();
	}
	@Override
	public void eraseArea(int x, int y, int w, int h, int trueBG)
	{
		// TODO Auto-generated method stub

	}
	@Override
	public void scroll(int y)
	{
		if(!isDisposed())
			execSWTSafe(() ->
			{
				Rectangle bounds = screenBuffer.getBounds();
				int w = bounds.width;
				int h = bounds.height;
				screenBuffer2GC.drawImage(screenBuffer, 0, y, w, h - y, 0, 0, w, h - y);
				screenBufferGC.drawImage(screenBuffer2, 0, 0);
			}, true);
	}
	@Override
	public void showStatusBar(ZSCIICharStream location, int scoreOrHours, int turnsOrMinutes, boolean isTimeGame)
	{
		// TODO Auto-generated method stub

	}
	@Override
	public void showChar(int zsciiChar, int font, int style, int trueFB, int trueBG, int x, int y)
	{
		if(!isDisposed())
			execSWTSafe(() ->
			{
				//TODO FG and BG
				screenBufferGC.drawText(String.valueOf(unicodeConv.zsciiToUnicodeNoNL(zsciiChar)), x, y);
			}, true);
	}
	@Override
	public void showPicture(int picture, int x, int y)
	{
		// TODO Auto-generated method stub

	}
	@Override
	public void erasePicture(int picture, int x, int y, int trueBG)
	{
		// TODO Auto-generated method stub

	}
	@Override
	public void flushScreen()
	{
		if(!isDisposed())
			execSWTSafe(this::redraw, true);
	}
	@Override
	public int nextInputChar()
	{
		if(isDisposed())
			return -1;
		if(inputBuffer.isEmpty())
		{
			while(inputBuffer.isEmpty() && !isDisposed());
			if(isDisposed())
				return -1;
		}
		return inputBuffer.poll();
	}

	private <T> T execSWTSafe(Supplier<T> exec)
	{
		if(isDisposed())
			return null;
		AtomicReference<T> result = new AtomicReference<>();
		execSWTSafe(() -> result.set(exec.get()), true);
		return result.get();
	}
	private void execSWTSafe(Runnable exec, boolean sync)
	{
		if(!isDisposed())
			if(sync)
				getDisplay().syncExec(exec);
			else
				getDisplay().asyncExec(exec);
	}
}