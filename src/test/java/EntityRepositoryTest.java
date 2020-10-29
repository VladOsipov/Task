import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;

public class EntityRepositoryTest {

	private EntityRepository entityRepository;

	private static final String FILE_NAME = "test.txt";
	private static final String SEPARATOR = " ";
	private static final int ID_INDEX = 0;
	private static final int DATA_INDEX = 1;
	
	private Map<Integer, String> testData;

	@Before
	public void init() throws IOException {
		testData = new HashMap<>();
		entityRepository = EntityRepository.getInstance();
	}

	@Test
	public void testSave() throws InterruptedException, IOException {
		int numberOfThreads = 10;
		ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
		CountDownLatch latch = new CountDownLatch(numberOfThreads);
		Runnable runnable = () -> {
			try {
				for (int i = 0; i < 100; i++) {
					String randomString = RandomStringUtils.randomAlphabetic(10);
					int id = entityRepository.save(randomString.getBytes());
					testData.put(id, randomString);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				latch.countDown();
			}
		};
		for (int i = 0; i < numberOfThreads; i++) {
			service.execute(runnable);
		}
		latch.await();
		verifyFileConsistency();
	}

	private void verifyFileConsistency() throws IOException {
		Map<Integer, String> fileData = new HashMap<>();

		try (BufferedReader inFile = new BufferedReader(new FileReader(FILE_NAME))) {
			String currentLine;
			while ((currentLine = inFile.readLine()) != null) {
				int id = Integer.parseInt(currentLine.split(SEPARATOR)[ID_INDEX]);
				String data = currentLine.split(SEPARATOR)[DATA_INDEX];
				fileData.put(id, data);
			}
		}

		assertEquals(fileData, testData);
	}

	@After
	public void clear() {
		File file = new File(FILE_NAME);
		file.delete();
	}

}
