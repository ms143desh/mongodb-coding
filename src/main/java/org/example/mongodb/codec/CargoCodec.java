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
import org.bson.types.ObjectId;
import org.example.mongodb.model.Cargo;

import com.mongodb.MongoClient;

public class CargoCodec implements CollectibleCodec<Cargo> {

	private final CodecRegistry registry;
    private final Codec<Document> documentCodec;
    private final CargoConverter converter;

    public CargoCodec() {
        this.registry = MongoClient.getDefaultCodecRegistry();
        this.documentCodec = this.registry.get(Document.class);
        this.converter = new CargoConverter();
    }

    public CargoCodec(Codec<Document> codec) {
        this.documentCodec = codec;
        this.registry = MongoClient.getDefaultCodecRegistry();
        this.converter = new CargoConverter();
    }

    public CargoCodec(CodecRegistry registry) {
        this.registry = registry;
        this.documentCodec = this.registry.get(Document.class);
        this.converter = new CargoConverter();
    }

    @Override
    public void encode(BsonWriter writer, Cargo cargo, EncoderContext encoderContext) {
        Document document = this.converter.convert(cargo);

        documentCodec.encode(writer, document, encoderContext);
    }

    @Override
    public Class<Cargo> getEncoderClass() {
        return Cargo.class;
    }

    @Override
    public Cargo decode(BsonReader reader, DecoderContext decoderContext) {
        Document document = documentCodec.decode(reader, decoderContext);
        return this.converter.convert(document);
    }

    @Override
    public Cargo generateIdIfAbsentFromDocument(Cargo cargo) {
        if (!documentHasId(cargo)) {
        	cargo.setId(ObjectId.get().toString());
        }

        return cargo;
    }

    @Override
    public boolean documentHasId(Cargo cargo) {
        return (cargo.getId() != null);
    }

    @Override
    public BsonValue getDocumentId(Cargo cargo)
    {
        if (!documentHasId(cargo)) {
            throw new IllegalStateException("The document does not contain an _id");
        }

        return new BsonString(cargo.getId());
    }
    
}
