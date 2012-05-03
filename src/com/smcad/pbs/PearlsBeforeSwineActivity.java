package com.smcad.pbs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ShareActionProvider;

public class PearlsBeforeSwineActivity extends Activity implements OnTouchListener {
		
	File imagesFolder = new File(Environment.getExternalStorageDirectory(), File.separator + "Pictures" + File.separator + "pbs" + File.separator);
	
	AspectRatioImageView imView;
	String imageUrl = "";
	public static final String KEY_DATE = "_date";
	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
	private static Date FIRST_COMIC_DATE;
	
	private ProgressDialog pd;
	private ShareActionProvider mShareActionProvider;
	private Date currentComicDate;
	
		/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.main);
		imView = (AspectRatioImageView) findViewById(R.id.imview);
		Bundle extras = getIntent().getExtras();
		
		try {
			FIRST_COMIC_DATE = dateFormat.parse("2006/07/24");
			currentComicDate = (Date) (extras != null ? dateFormat.parse(getIntent().getStringExtra(KEY_DATE)) : new Date());
			if (!currentComicDate.before(Calendar.getInstance().getTime())) {
				currentComicDate = Calendar.getInstance().getTime();
			}
			loadComicFromDate(currentComicDate);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ActionBar actionBar = getActionBar();
		actionBar.setTitle(dateFormat.format(currentComicDate));
		
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				cachePrevAndNextImages();
			}
		};
		new Thread(runnable).start();
	}
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.share, menu);
		
		// set up a listener for the shuffle item
		final MenuItem shuffle = (MenuItem) menu.findItem(R.id.menu_shuffle);
		shuffle.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			// on selecting show progress spinner for 1s
			public boolean onMenuItemClick(MenuItem item) {
				comicForDate(getRandomDateBetween(FIRST_COMIC_DATE, Calendar.getInstance().getTime()));
				return true;
			}
		});
		
		// set up a listener for the refresh item
		final MenuItem purge = (MenuItem) menu.findItem(R.id.menu_purge);
		purge.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			// on selecting show progress spinner for 1s
			public boolean onMenuItemClick(MenuItem item) {
				pd = ProgressDialog.show(PearlsBeforeSwineActivity.this, "Working..", "Purging Images", true, false);
				Runnable runnable = new Runnable() {
					@Override
					public void run() {
						deleteDirectory(imagesFolder);
						handler.sendEmptyMessage(0);
					}
				};
				new Thread(runnable).start();
				
				return true;
			}
		});
		
	    mShareActionProvider = (ShareActionProvider) menu.findItem(R.id.menu_share).getActionProvider();
	    mShareActionProvider.setShareIntent(getDefaultShareIntent());
	    
	    return true;
	}
	
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
                pd.dismiss();
        }
    };
	
	public Intent getDefaultShareIntent() {
		Intent shareIntent = new Intent(Intent.ACTION_SEND);
		shareIntent.setType("image/png");
		Uri uri = Uri.fromFile(new File(imagesFolder.getAbsolutePath() + File.separator + dateFormat.format(currentComicDate) + ".png"));
		System.out.println("uri = " + uri.toString());
		shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
		return shareIntent;
		
	}
	
	private Date getRandomDateBetween(Date from, Date to) {
		Calendar cal = Calendar.getInstance();

		cal.setTime(from);
		BigDecimal decFrom = new BigDecimal(cal.getTimeInMillis());

		cal.setTime(to);
		BigDecimal decTo = new BigDecimal(cal.getTimeInMillis());

		BigDecimal selisih = decTo.subtract(decFrom);
		BigDecimal factor = selisih.multiply(new BigDecimal(Math.random()));

		return new Date((factor.add(decFrom)).longValue());
	}
	
	public void nextComic() {
		Intent i = new Intent(
				PearlsBeforeSwineActivity.this,
				PearlsBeforeSwineActivity.class);
		i.putExtra(KEY_DATE, dateFormat.format(dateOffset(1)));
		
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(i);
		overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
	}
	
	public void comicForDate(Date date) {
		Intent i = new Intent(
				PearlsBeforeSwineActivity.this,
				PearlsBeforeSwineActivity.class);
		i.putExtra(KEY_DATE, dateFormat.format(date));
		
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(i);
		overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
	}
	
	public void prevComic() {
		Intent i = new Intent(
				PearlsBeforeSwineActivity.this,
				PearlsBeforeSwineActivity.class);
		i.putExtra(KEY_DATE, dateFormat.format(dateOffset(-1)));
		
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(i);
		
		overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
	}

	public Date dateOffset(int offset) {
		Calendar c1 = Calendar.getInstance();
		Date curr = currentComicDate;
		c1.setTime(curr);
		c1.add(Calendar.DAY_OF_MONTH, offset);
		return c1.getTime();
	}

	public void loadComicFromDate(final Date date) {
		// load comic from SD card
		Bitmap image = loadComicFromSD(dateFormat.format(date));
		
		if (image != null) {
			System.out.println("loading comic from SD ...");
			imView.setImageBitmap(image);
		} else {
			loadComic(getComicUrlFromDate(date == null ? new Date() : date));
		}

	}
	
	public Bitmap loadComicFromSD(String filename) {
		filename = filename.replaceAll("/", ".");
		Bitmap bmp = BitmapFactory.decodeFile(imagesFolder.getAbsolutePath() + File.separator + filename + ".png");
		return bmp; 
	}
	
	public boolean writeComicToSD(String filename, Bitmap image) {
		if (image == null) { 
			return false;
		}
		imagesFolder.mkdirs();
		try {
			filename = filename.replaceAll("/", ".");
			FileOutputStream fos;
			fos = new FileOutputStream(imagesFolder.getAbsolutePath()
					+ File.separator + filename + ".png");
			image.compress(CompressFormat.PNG, 100, fos);
			fos.flush();
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
        
        return new File(imagesFolder.getAbsolutePath() + File.separator +  filename).canRead();
	}

	private String getComicUrlFromDate(Date date) {
		System.out.println("getComicUrlFromDate(" + dateFormat.format(date) + ")");	
		return "http://www.gocomics.com/pearlsbeforeswine/" + dateFormat.format(date);
	}

	public void comicLoadingFailed(final String reason) {
		handler.post(new Runnable() {
			public void run() {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						PearlsBeforeSwineActivity.this);
				builder.setMessage("Comic loading failed: " + reason);
				AlertDialog alert = builder.create();
				alert.show();
			}
		});
	}
	
	public void cachePrevAndNextImages() {
		try {
			Date nextDate = dateOffset(1);
			if (!nextDate.before(Calendar.getInstance().getTime())) {
				nextDate = Calendar.getInstance().getTime();
			}
			String nextDateString = dateFormat.format(nextDate);
			Bitmap nextImage = loadComicFromSD(nextDateString);
			if (nextImage == null) {
				nextImage = loadImage(new URL(ComicExtractor.getComicImageUrlFromPageScrape(getComicUrlFromDate(nextDate))));
				if (nextImage != null) {
					writeComicToSD(nextDateString, nextImage);
				}
			}
			
			
			Date prevDate = dateOffset(-1);
			String prevDateString = dateFormat.format(prevDate);
			Bitmap prevImage = loadComicFromSD(prevDateString);
			if (prevImage == null) {
				prevImage = loadImage(new URL(ComicExtractor.getComicImageUrlFromPageScrape(getComicUrlFromDate(dateOffset(-1)))));
				if (prevImage != null) {
					writeComicToSD(prevDateString, prevImage);
				}
			}

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public Bitmap loadImage(URL myFileUrl) { 
		Bitmap bmImg = null;
		try {

			HttpURLConnection conn = (HttpURLConnection) myFileUrl.openConnection();
			conn.setDoInput(true);
			conn.connect();
			InputStream is = conn.getInputStream();
			bmImg = BitmapFactory.decodeStream(is);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bmImg;
	}
	
	
	void loadComic(final String fileUrl) {
		pd = ProgressDialog.show(PearlsBeforeSwineActivity.this, "Working..", "Download Image", true, false);
		
		Runnable runnable = new Runnable() {
			@Override
			public void run() {

				try {
					String comicImageUrl = ComicExtractor.getComicImageUrlFromPageScrape(fileUrl);
					if (comicImageUrl != null) {
						final Bitmap image = loadImage(new URL(comicImageUrl));
						runOnUiThread(new Runnable() {
						     public void run() {
						    	 imView.setImageBitmap(image);
						    	 handler.sendEmptyMessage(0);
						     }
						});
					}
					else {
						comicLoadingFailed("Unable to scrape HTML page");
						handler.sendEmptyMessage(0);
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					handler.sendEmptyMessage(0);
				}
				
			}
		};
		new Thread(runnable).start();
		
		
		
//		
//		DownloadWebPageTask task = new DownloadWebPageTask(imView);
//		task.execute(new String[] { fileUrl });
		
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent e) {
		if (e.getAction() == MotionEvent.ACTION_UP) {
			int x = (int)e.getX();
			System.out.println("x = " + x);
		
	        Display display = getWindowManager().getDefaultDisplay();
	        Point size = new Point();
	        display.getSize(size);
	        int width = size.x;
	        
	        if (x > (width / 2)) {
	        	nextComic();
	        }
	        else {
	        	prevComic();
	        }
		}
        
		return true;
	}
	
	static public boolean deleteDirectory(File path) {
	    if( path.exists() ) {
	      File[] files = path.listFiles();
	      if (files == null) {
	          return true;
	      }
	      for(int i=0; i<files.length; i++) {
	         if(files[i].isDirectory()) {
	           deleteDirectory(files[i]);
	         }
	         else {
	           files[i].delete();
	         }
	      }
	    }
	    return( path.delete() );
	  }

	@Override
	public boolean onTouch(View v, MotionEvent e) {
		System.out.println("onTouch");
		return true;
	}
}