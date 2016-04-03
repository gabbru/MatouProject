package fr.upem.net.tcp.server.readers;

import java.nio.ByteBuffer;

import fr.upem.net.tcp.server.DataPacketRead;

public class ReaderString implements Reader {
	private int sizeString;
	private DataPacketRead data;
	private StatusReaderTreatment status = StatusReaderTreatment.BEGIN;
	private Reader reader;
	private final int concernedData;

	public ReaderString(Reader reader, int concernedData) {
		this.reader = reader;
		this.concernedData = concernedData;// HANDLE TO KNOW IF WE ARE READING
											// LOGIN FROM SRC OR DATA FROM DEST
	}

	public ReaderString(int concernedData) {
		this.concernedData = concernedData;
		data = new DataPacketRead();
	}

	/**
	 * treatment:
	 * 
	 * retrieve sizeLogin and login from the {@link ByteBuffer} in and save
	 * those elements in the field "data"
	 * 
	 * @param in
	 */
	private void treatment(ByteBuffer in) {
		in.flip();
		int oldLimit = in.limit();
		in.limit(sizeString);
		switch (concernedData) {
		case SRC_DATA:
			data.setLoginSrc(UTF_8.decode(in).toString());
			break;
		case DEST_DATA:
			data.setLoginDst(UTF_8.decode(in).toString());
			break;
		case DEST_DATA_ADR:
			data.setAdrDest(UTF_8.decode(in).toString());
			break;
		}
		in.limit(oldLimit);
		in.compact();
	}

	@Override
	public StatusProcessing process(ByteBuffer in) {
		switch (status) {

		case BEGIN:
			if (reader == null) {
				status = StatusReaderTreatment.READER_USED;
			} else {
				StatusProcessing statusCalledReader = reader.process(in);
				if (statusCalledReader == StatusProcessing.DONE) {
					data = reader.get();
					status = StatusReaderTreatment.READER_USED;
				} else {
					return statusCalledReader;// REFILL OR ERROR
				}
			}
		case READER_USED:
			if (in.position() >= Integer.BYTES) {
				in.flip();
				sizeString = in.getInt();

				switch (concernedData) {
				case SRC_DATA:
					data.setSizeLoginSrc(sizeString);
					break;
				case DEST_DATA:
					data.setSizeLoginDst(sizeString);
					break;
				case DEST_DATA_ADR:
					data.setSizeAdressDst(sizeString);
					break;
				}

				in.compact();
				status = StatusReaderTreatment.SIZE_STRING_KNOWN;
			}// on envel volontairement le break dans le but de passer a la size
				// suivante
		case SIZE_STRING_KNOWN:
			if (in.position() >= sizeString) {
				treatment(in);
				return StatusProcessing.DONE;
			}
		}

		// c quand qu'on a error
		return StatusProcessing.REFILL;
	}

	@Override
	public DataPacketRead get() {

		return data;
	}

	@Override
	public void reset() {
		if (reader != null) {
			reader.reset();
		}
		// clear data ??

	}

}
