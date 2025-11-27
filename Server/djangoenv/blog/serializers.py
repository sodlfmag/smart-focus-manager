from blog.models import Post
from rest_framework import serializers
from django.contrib.auth.models import User

class PostSerializer(serializers.HyperlinkedModelSerializer):
    author = serializers.PrimaryKeyRelatedField(queryset=User.objects.all())
    image = serializers.ImageField(required=False, allow_null=True)

    class Meta:
        model = Post
        fields = ('author', 'title', 'text', 'created_date', 'published_date', 'image')

    def to_representation(self, instance):
        data = super().to_representation(instance)
        image = instance.image
        if image:
            request = self.context.get('request')
            url = image.url
            if request:
                url = request.build_absolute_uri(url)
            data['image'] = url
        else:
            data['image'] = None
        return data