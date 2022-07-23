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
import org.example.mongodb.model.City;

import com.mongodb.MongoClient;

public class CityCodec implements CollectibleCodec<City> {

    private final CodecRegistry registry;
    private final Codec<Document> documentCodec;
    private final CityConverter converter;

    public CityCodec() {
        this.registry = MongoClient.getDefaultCodecRegistry();
        this.documentCodec = this.registry.get(Document.class);
        this.converter = new CityConverter();
    }

    public CityCodec(Codec<Document> codec) {
        this.documentCodec = codec;
        this.registry = MongoClient.getDefaultCodecRegistry();
        this.converter = new CityConverter();
    }

    public CityCodec(CodecRegistry registry) {
        this.registry = registry;
        this.documentCodec = this.registry.get(Document.class);
        this.converter = new CityConverter();
    }

    @Override
    public void encode(BsonWriter writer, City city, EncoderContext encoderContext) {
        Document document = this.converter.convert(city);

        documentCodec.encode(writer, document, encoderContext);
    }

    @Override
    public Class<City> getEncoderClass() {
        return City.class;
    }

    @Override
    public City decode(BsonReader reader, DecoderContext decoderContext) {
        Document document = documentCodec.decode(reader, decoderContext);
        return this.converter.convert(document);
    }

    @Override
    public City generateIdIfAbsentFromDocument(City city) {
        if (!documentHasId(city)) {
        	throw new IllegalStateException("The document id should be provided");
        }

        return city;
    }

    @Override
    public boolean documentHasId(City city) {
        return (city.getName() != null);
    }

    @Override
    public BsonValue getDocumentId(City city)
    {
        if (!documentHasId(city)) {
            throw new IllegalStateException("The document does not contain an _id");
        }

        return new BsonString(city.getName());
    }

}