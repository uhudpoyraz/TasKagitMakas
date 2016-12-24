package com.taskagitmakas.form;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.omg.IOP.Codec;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG2;

public class CamRecorder {

	private VideoCapture videoCapture;

	public JFrame Window;
	private ImageIcon image;
	private JLabel imageLable;
	private Boolean sizeCustom = false;
	private int Height, Width;
	private Rect boundRect;

	private static final int SAMPLE_NUM = 1;

	private Imshow im, im2, im3, im4;
	private Point[][] samplePoints = null;
	private double[][] avgColor = null;
	private Mat background, grayImage, Image;

	private int squareLen = 50;

	private ColorBlobDetector mDetector;
	private Mat mSpectrum;
	int numberOfFingers = 0;
	private Scalar mBlobColorHsv;
	private Scalar mBlobColorRgba;

	public VideoCapture getVideoCapture() {
		return videoCapture;
	}

	public void setVideoCapture(VideoCapture videoCapture) {
		this.videoCapture = videoCapture;
	}

	public CamRecorder() {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		videoCapture = new VideoCapture(0);
		System.out.println(videoCapture.get(12));
		im = new Imshow("HSVsmall");
		im2 = new Imshow("Gray");
		im3 = new Imshow("HSVFULL");

		im.Window.setResizable(true);
		im.Window.setResizable(true);
		im.Window.setResizable(true);

		background = new Mat();
		Image = new Mat();

		videoCapture.read(background);
		Core.flip(background, background, 1);
		

		int cols, rows;

		cols = background.cols();
		rows = background.rows();
		System.out.println(background.size());
		System.out.println(cols);
		samplePoints = new Point[SAMPLE_NUM][2];
		samplePoints = new Point[SAMPLE_NUM][2];
		for (int i = 0; i < SAMPLE_NUM; i++) {
			for (int j = 0; j < 2; j++) {
				samplePoints[i][j] = new Point();
			}
		}
		samplePoints[0][0].x = cols / 2.5;
		samplePoints[0][0].y = rows / 2.5;

		for (int i = 0; i < SAMPLE_NUM; i++) {
			samplePoints[i][1].x = samplePoints[i][0].x + squareLen / 2;
			samplePoints[i][1].y = samplePoints[i][0].y + squareLen / 2;
		}

		avgColor = new double[SAMPLE_NUM][3];
		mDetector = new ColorBlobDetector();
		mSpectrum = new Mat();
		mBlobColorHsv = new Scalar(255);
		mBlobColorRgba = new Scalar(255);
	}

	private int LearningTime = 0;

	public Mat startRecord() {

		Mat m = new Mat();
		Mat c = new Mat();

		this.videoCapture.read(m);
		 
		Core.flip(m, m, 1);

		Imgproc.cvtColor(m, c, Imgproc.COLOR_RGB2RGBA);
		Imgproc.GaussianBlur(c, c, new Size(9, 9), 5, 5);

		for (int i = 0; i < SAMPLE_NUM; i++) {

			Core.rectangle(m, samplePoints[i][0], samplePoints[i][1], new Scalar(47, 255, 6), 1);
		}

		Rect touchedRect = new Rect();

		int x = (int) (samplePoints[0][0].x + squareLen / 2);
		int y = (int) (samplePoints[0][0].y + squareLen / 2);
		touchedRect.x = x;
		touchedRect.y = y;

		touchedRect.width = (x + 50 < c.cols()) ? x + 50 - touchedRect.x : c.cols() - touchedRect.x;
		touchedRect.height = (y + 50 < c.rows()) ? y + 50 - touchedRect.y : c.rows() - touchedRect.y;

		Mat touchedRegionRgba = c.submat(touchedRect);

		Mat touchedRegionHsv = new Mat();
		Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL, 3);
		im.showImage(touchedRegionHsv);

		mBlobColorHsv = Core.sumElems(touchedRegionHsv);
		int pointCount = touchedRect.width * touchedRect.height;
		for (int i = 0; i < mBlobColorHsv.val.length; i++)
			mBlobColorHsv.val[i] /= pointCount;

		mDetector.setHsvColor(mBlobColorHsv);
		Imgproc.resize(mDetector.getSpectrum(), mSpectrum, new Size(200, 64));
		return (m);

	}

	double iThreshold = 0;

	public Mat filterSkinColor() {

		Mat m = new Mat();
		Mat c = new Mat();
		Mat b = new Mat();

		this.videoCapture.read(m);
		Core.flip(m, m, 1);
	 
		m.copyTo(Image);
		m.copyTo(c);
		Imgproc.cvtColor(m, c, Imgproc.COLOR_RGB2RGBA);
		Imgproc.GaussianBlur(c, c, new Size(9, 9), 5, 5);

		List<MatOfPoint> contours = mDetector.getContours();
		mDetector.process(c);
		RotatedRect rect = Imgproc.minAreaRect(new MatOfPoint2f(contours.get(0).toArray()));

		im3.showImage(mDetector.mHsvMat);

		double boundWidth = rect.size.width;
		double boundHeight = rect.size.height;
		int boundPos = 0;

		for (int i = 1; i < contours.size(); i++) {
			rect = Imgproc.minAreaRect(new MatOfPoint2f(contours.get(i).toArray()));
			if (rect.size.width * rect.size.height > boundWidth * boundHeight) {
				boundWidth = rect.size.width;
				boundHeight = rect.size.height;
				boundPos = i;
			}
		}

		Rect boundRect = Imgproc.boundingRect(new MatOfPoint(contours.get(boundPos).toArray()));
		Core.rectangle(m, boundRect.tl(), boundRect.br(), new Scalar(255, 255, 255), 2, 8, 0);

		int rectHeightThresh = 0;
		double a = boundRect.br().y - boundRect.tl().y;
		a = a * 0.7;
		a = boundRect.tl().y + a;
		Core.rectangle(m, boundRect.tl(), new Point(boundRect.br().x, a), new Scalar(0, 255, 0), 2, 8, 0);

		MatOfPoint2f pointMat = new MatOfPoint2f();
		Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(boundPos).toArray()), pointMat, 3, true);
		contours.set(boundPos, new MatOfPoint(pointMat.toArray()));

		MatOfInt hull = new MatOfInt();
		MatOfInt4 convexDefect = new MatOfInt4();
		Imgproc.convexHull(new MatOfPoint(contours.get(boundPos).toArray()), hull);

		if (hull.toArray().length < 3)
			return (m);

		Imgproc.convexityDefects(new MatOfPoint(contours.get(boundPos).toArray()), hull, convexDefect);

		List<MatOfPoint> hullPoints = new LinkedList<MatOfPoint>();
		List<Point> listPo = new LinkedList<Point>();
		for (int j = 0; j < hull.toList().size(); j++) {
			listPo.add(contours.get(boundPos).toList().get(hull.toList().get(j)));
		}

		MatOfPoint e = new MatOfPoint();
		e.fromList(listPo);
		hullPoints.add(e);

		Imgproc.drawContours(m, hullPoints, -1, new Scalar(0, 255, 0), 3);

		int defectsTotal = (int) convexDefect.total();

		im2.showImage(mDetector.mGray);

		// mDetector.mGray.copyTo(grayImage);
		return (m);

	}

	
	public Mat train() {

		Mat m = new Mat();
		Mat c = new Mat();
		Mat b = new Mat();

		this.videoCapture.read(m);
		Core.flip(m, m, 1);
	 
		m.copyTo(Image);
		m.copyTo(c);
		Imgproc.cvtColor(m, c, Imgproc.COLOR_RGB2RGBA);
		Imgproc.GaussianBlur(c, c, new Size(9, 9), 5, 5);

		List<MatOfPoint> contours = mDetector.getContours();
		mDetector.process(c);
		RotatedRect rect = Imgproc.minAreaRect(new MatOfPoint2f(contours.get(0).toArray()));
		double boundWidth = rect.size.width;
		double boundHeight = rect.size.height;
		int boundPos = 0;

		for (int i = 1; i < contours.size(); i++) {
			rect = Imgproc.minAreaRect(new MatOfPoint2f(contours.get(i).toArray()));
			if (rect.size.width * rect.size.height > boundWidth * boundHeight) {
				boundWidth = rect.size.width;
				boundHeight = rect.size.height;
				boundPos = i;
			}
		}

		boundRect = Imgproc.boundingRect(new MatOfPoint(contours.get(boundPos).toArray()));
		Core.rectangle(m, boundRect.tl(), boundRect.br(), new Scalar(255, 255, 255), 2, 8, 0);

		int rectHeightThresh = 0;
		double a = boundRect.br().y - boundRect.tl().y;
		a = a * 0.7;
		a = boundRect.tl().y + a;
		Core.rectangle(m, boundRect.tl(), new Point(boundRect.br().x, a), new Scalar(0, 255, 0), 2, 8, 0);

		MatOfPoint2f pointMat = new MatOfPoint2f();
		Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(boundPos).toArray()), pointMat, 3, true);
		contours.set(boundPos, new MatOfPoint(pointMat.toArray()));

		MatOfInt hull = new MatOfInt();
		MatOfInt4 convexDefect = new MatOfInt4();
		Imgproc.convexHull(new MatOfPoint(contours.get(boundPos).toArray()), hull);

		if (hull.toArray().length < 3)
			return m;

		Imgproc.convexityDefects(new MatOfPoint(contours.get(boundPos).toArray()), hull, convexDefect);

		List<MatOfPoint> hullPoints = new LinkedList<MatOfPoint>();
		List<Point> listPo = new LinkedList<Point>();
		for (int j = 0; j < hull.toList().size(); j++) {
			listPo.add(contours.get(boundPos).toList().get(hull.toList().get(j)));
		}

		MatOfPoint e = new MatOfPoint();
		e.fromList(listPo);
		hullPoints.add(e);

		Imgproc.drawContours(m, hullPoints, -1, new Scalar(0, 255, 0), 3);

		int defectsTotal = (int) convexDefect.total();

			// mDetector.mGray.copyTo(grayImage);
		return m;

	}
	
	
	public BufferedImage toBufferedImage(Mat m) {
		int type = BufferedImage.TYPE_BYTE_GRAY;
		if (m.channels() > 1) {
			type = BufferedImage.TYPE_3BYTE_BGR;
		}

		int bufferSize = m.channels() * m.cols() * m.rows();
		byte[] b = new byte[bufferSize];
		m.get(0, 0, b); // get all the pixels
		BufferedImage image = new BufferedImage(m.cols(), m.rows(), type);
		final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

		System.arraycopy(b, 0, targetPixels, 0, b.length);

		return image;

	}

	public void closeCam() {

		this.videoCapture.release();
	}

	public Mat saveImage() {
		Mat frameFromCam=new Mat();
		frameFromCam=Image.submat(boundRect);
		return frameFromCam;
	}
	public Mat getImage(){
		
		return this.Image;
	}

}
