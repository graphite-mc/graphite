struct BlurUniforms {
    direction: vec2<f32>,
    _padding: vec2<f32>,
};

struct VertexOutput {
    @builtin(position) position: vec4<f32>,
    @location(0) uv: vec2<f32>,
};

@group(0) @binding(0) var texture_sampler: sampler;
@group(0) @binding(1) var source_texture: texture_2d<f32>;
@group(0) @binding(2) var<uniform> blur_uniforms: BlurUniforms;

@vertex
fn vs_main(@builtin(vertex_index) vertex_index: u32) -> VertexOutput {
    var output: VertexOutput;

    let x = f32((vertex_index << 1u) & 2u);
    let y = f32(vertex_index & 2u);

    output.position = vec4<f32>(x * 2.0 - 1.0, 1.0 - y * 2.0, 0.0, 1.0);
    output.uv = vec2<f32>(x, y);

    return output;
}

@fragment
fn fs_main(input: VertexOutput) -> @location(0) vec4<f32> {
    let tex_size = vec2<f32>(textureDimensions(source_texture, 0));
    let dir = blur_uniforms.direction;

    var color = textureSample(source_texture, texture_sampler, input.uv) * 0.227027;

    color += textureSample(source_texture, texture_sampler, input.uv + dir * 1.0) * 0.1945946;
    color += textureSample(source_texture, texture_sampler, input.uv - dir * 1.0) * 0.1945946;

    color += textureSample(source_texture, texture_sampler, input.uv + dir * 2.0) * 0.1216216;
    color += textureSample(source_texture, texture_sampler, input.uv - dir * 2.0) * 0.1216216;

    color += textureSample(source_texture, texture_sampler, input.uv + dir * 3.0) * 0.054054;
    color += textureSample(source_texture, texture_sampler, input.uv - dir * 3.0) * 0.054054;

    color += textureSample(source_texture, texture_sampler, input.uv + dir * 4.0) * 0.016216;
    color += textureSample(source_texture, texture_sampler, input.uv - dir * 4.0) * 0.016216;

    return color;
}