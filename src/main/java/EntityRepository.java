import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class EntityRepository {

	private static final String FILE_NAME = "test.txt";

	private static final ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
	private static final Lock readLock = readWriteLock.readLock();
	private static final Lock writeLock = readWriteLock.writeLock();
	private static final String SEPARATOR = " ";
	private static final int ID_INDEX = 0;
	private static final int DATA_INDEX = 1;
	private static final String END_LINE = "\n";

	private static EntityRepository instance;
	private static AtomicInteger lastId;
	private static Lock initializationLock = new ReentrantLock();

	private EntityRepository() {}

	public static EntityRepository getInstance() throws IOException {
		initializationLock.lock();
		try {
			if (instance == null) {
				boolean fileCreated = new File(FILE_NAME).createNewFile();
				lastId = new AtomicInteger(fileCreated ? 0 : getMaxId());
				instance = new EntityRepository();
			}
		} finally {
			initializationLock.unlock();
		}
		return instance;
	}

	private static int getMaxId() throws IOException {
		int maxId = 0;
		try (BufferedReader inFile = new BufferedReader(new FileReader(FILE_NAME))) {
			String currentLine;
			while ((currentLine = inFile.readLine()) != null) {
				int currentLineId = Integer.parseInt(currentLine.split(SEPARATOR)[ID_INDEX]);
				if (currentLineId > maxId) {
					maxId = currentLineId;
				}
			}
		}
		return maxId;
	}

	public int save(byte[] buffer) throws IOException {
		writeLock.lock();
		try (FileOutputStream fos = new FileOutputStream(FILE_NAME, true)) {
			int currentId = lastId.incrementAndGet();
			fos.write(String.valueOf(currentId).getBytes());
			fos.write(SEPARATOR.getBytes());
			fos.write(buffer);
			fos.write(END_LINE.getBytes());
			return currentId;
		} finally {
			writeLock.unlock();
		}
	}

	public byte[] get(int id) throws IOException {
		readLock.lock();
		try (BufferedReader bufferedReader = new BufferedReader(new FileReader(FILE_NAME))) {
			String currentLine;
			while ((currentLine = bufferedReader.readLine()) != null) {
				String[] currentLineParts = currentLine.split(SEPARATOR);
				int currentLineId = Integer.parseInt(currentLineParts[ID_INDEX]);
				if (currentLineId == id) {
					return currentLineParts[DATA_INDEX].getBytes();
				}
			}
		} finally {
			readLock.unlock();
		}
		return new byte[]{};
	}

}
