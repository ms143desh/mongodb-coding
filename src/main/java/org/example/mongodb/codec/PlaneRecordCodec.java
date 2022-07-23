package org.example.mongodb.codec;

import org.bson.BsonReader;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.example.mongodb.model.PlaneRecord;

import com.mongodb.MongoClient;

public class PlaneRecordCodec implements CollectibleCodec<PlaneRecord> {

    private final CodecRegistry registry;
    private final Codec<Document> documentCodec;
    private final PlaneRecordConverter converter;

    public PlaneRecordCodec() {
        this.registry = MongoClient.getDefaultCodecRegistry();
        this.documentCodec = this.registry.get(Document.class);
        this.converter = new PlaneRecordConverter();
    }

    public PlaneRecordCodec(Codec<Document> codec) {
        this.documentCodec = codec;
        this.registry = MongoClient.getDefaultCodecRegistry();
        this.converter = new PlaneRecordConverter();
    }

    public PlaneRecordCodec(CodecRegistry registry) {
        this.registry = registry;
        this.documentCodec = this.registry.get(Document.class);
        this.converter = new PlaneRecordConverter();
    }

    @Override
    public void encode( BsonWriter writer, PlaneRecord planeRecord, EncoderContext encoderContext) {
        Document document = this.converter.convert(planeRecord);

        documentCodec.encode(writer, document, encoderContext);
    }

    @Override
    public Class<PlaneRecord> getEncoderClass() {
        return PlaneRecord.class;
    }

    @Override
    public PlaneRecord decode(BsonReader reader, DecoderContext decoderContext) {
        Document document = documentCodec.decode(reader, decoderContext);
        return this.converter.convert(document);
    }

    @Override
    public PlaneRecord generateIdIfAbsentFromDocument(PlaneRecord planeRecord) {
        if (!documentHasId(planeRecord)) {
            throw new IllegalStateException("The document id should be provided");
        }

        return planeRecord;
    }

    @Override
    public boolean documentHasId(PlaneRecord planeRecord) {
        return (planeRecord.getCallsign() != null);
    }

    @Override
    public BsonValue getDocumentId(PlaneRecord planeRecord)
    {
        if (!documentHasId(planeRecord)) {
            throw new IllegalStateException("The document does not contain an _id");
        }

        return new BsonString(planeRecord.getCallsign());
    }

}