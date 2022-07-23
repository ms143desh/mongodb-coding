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
import org.example.mongodb.model.Plane;

import com.mongodb.MongoClient;

public class PlaneCodec implements CollectibleCodec<Plane> {

    private final CodecRegistry registry;
    private final Codec<Document> documentCodec;
    private final PlaneConverter converter;

    public PlaneCodec() {
        this.registry = MongoClient.getDefaultCodecRegistry();
        this.documentCodec = this.registry.get(Document.class);
        this.converter = new PlaneConverter();
    }

    public PlaneCodec(Codec<Document> codec) {
        this.documentCodec = codec;
        this.registry = MongoClient.getDefaultCodecRegistry();
        this.converter = new PlaneConverter();
    }

    public PlaneCodec(CodecRegistry registry) {
        this.registry = registry;
        this.documentCodec = this.registry.get(Document.class);
        this.converter = new PlaneConverter();
    }

    @Override
    public void encode( BsonWriter writer, Plane plane, EncoderContext encoderContext) {
        Document document = this.converter.convert(plane);

        documentCodec.encode(writer, document, encoderContext);
    }

    @Override
    public Class<Plane> getEncoderClass() {
        return Plane.class;
    }

    @Override
    public Plane decode(BsonReader reader, DecoderContext decoderContext) {
        Document document = documentCodec.decode(reader, decoderContext);
        return this.converter.convert(document);
    }

    @Override
    public Plane generateIdIfAbsentFromDocument(Plane plane) {
        if (!documentHasId(plane)) {
            throw new IllegalStateException("The document id should be provided");
        }

        return plane;
    }

    @Override
    public boolean documentHasId(Plane plane) {
        return (plane.getCallsign() != null);
    }

    @Override
    public BsonValue getDocumentId(Plane plane)
    {
        if (!documentHasId(plane)) {
            throw new IllegalStateException("The document does not contain an _id");
        }

        return new BsonString(plane.getCallsign());
    }

}