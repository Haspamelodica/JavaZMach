package net.haspamelodica.javazmach.io.swt;

import static net.haspamelodica.javazmach.JavaZMachRunner.createInterpreter;
import static net.haspamelodica.javazmach.JavaZMachRunner.readConfigFromArgs;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import net.haspamelodica.javazmach.GlobalConfig;
import net.haspamelodica.javazmach.core.ZInterpreter;
import net.haspamelodica.javazmach.core.text.UnicodeZSCIIConverter;
import net.haspamelodica.javazmach.core.text.UnicodeZSCIIConverterNoSpecialChars;

public class JavaZMachSWT
{
	public static void main(String[] args) throws IOException
	{
		GlobalConfig config = readConfigFromArgs(args);
		UnicodeZSCIIConverter unicodeConv = new UnicodeZSCIIConverterNoSpecialChars(config);
		Display display = new Display();
		Shell shell = new Shell(SWT.CLOSE | SWT.TITLE | SWT.MIN);
		shell.setLayout(new FillLayout());
		ZInterpreter zInterpreter = createInterpreter(config, new SWTVideoCard(shell, SWT.NONE, config, unicodeConv), unicodeConv);
		shell.setSize(400, 400);
		shell.layout();
		shell.open();
		AtomicBoolean zmachineRunning = new AtomicBoolean();
		new Thread(() ->
		{
			zmachineRunning.set(true);
			zInterpreter.reset();
			try
			{
				while(zInterpreter.step() && zmachineRunning.get());
			} catch(Throwable x)
			{
				display.syncExec(() ->
				{
					MessageBox box = new MessageBox(shell, SWT.ERROR);
					box.setText("Error");
					box.setMessage("An interpreter exception occurred:\n" + x);
					box.open();
				});
			}
			zmachineRunning.set(false);
		}, "Z-machine").start();
		while(!zmachineRunning.get());
		while(!shell.isDisposed() && zmachineRunning.get())
			if(!display.readAndDispatch())
				display.sleep();
		zmachineRunning.set(false);
		display.dispose();
	}
}